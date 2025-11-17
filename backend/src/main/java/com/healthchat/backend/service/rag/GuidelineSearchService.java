package com.healthchat.backend.service.rag;

import com.healthchat.backend.config.GeminiClient;
import com.healthchat.backend.entity.GuidelineChunk;
import com.healthchat.backend.repository.GuidelineChunkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class GuidelineSearchService {

    private final GeminiClient gemini;
    private final GuidelineChunkRepository repo;

    /** 최종 반환할 문헌 개수 (문헌별 1개씩) */
    private static final int TOP_K = 2;

    /** 문헌 텍스트 길이 축소 기준 */
    private static final int MAX_TEXT_LEN = 350;

    /**
     * 사용자 질문 → 임베딩 → 청크 검색 → 문헌별 상위 1개씩 → 상위 K개 반환
     */
    public List<RetrievedChunk> searchRelevantChunks(String query) {

        // 0. 로그 (쿼리 미리 보기)
        log.info("🔎 [RAG] 검색 시작, queryPreview={}", preview(query, 200));

        // 1. query embedding 생성
        float[] queryEmbedding = gemini.embed(query);
        if (queryEmbedding == null || queryEmbedding.length == 0) {
            log.error("⚠ [RAG] 쿼리 임베딩 실패 → 빈 결과 반환");
            return List.of();
        }

        // 2. 모든 저장된 chunks 가져오기
        List<GuidelineChunk> all = repo.findAll();
        if (all.isEmpty()) {
            log.warn("⚠ [RAG] guideline_chunks 테이블이 비어 있음");
            return List.of();
        }

        // 3. 청크별 similarity + 목표 기반 가중치 계산
        List<ChunkScore> scored = new ArrayList<>();

        for (GuidelineChunk chunk : all) {
            float[] chunkVector = EmbeddingUtil.toFloatArray(chunk.getEmbedding());

            double rawSim = cosineSimilarity(queryEmbedding, chunkVector);
            double boostedSim = applyGoalAwareBoost(query, chunk.getSource(), rawSim);

            scored.add(new ChunkScore(chunk, rawSim, boostedSim));
        }

        // 3-1. boosted similarity 기준 상위 10개 로그 출력
        scored.stream()
                .sorted(Comparator.comparingDouble((ChunkScore c) -> c.boostedSim).reversed())
                .limit(10)
                .forEach(c -> log.info(
                        "   ▸ [raw={}] [boosted={}] [src={}] preview={}",
                        String.format("%.4f", c.rawSim),
                        String.format("%.4f", c.boostedSim),
                        c.chunk.getSource(),
                        preview(c.chunk.getText(), 60)
                ));

        // 4. 문헌(source)별로 boostedSim 가장 높은 청크 선택
        Map<String, RetrievedChunk> bestBySource = new HashMap<>();

        for (ChunkScore c : scored) {
            String source = c.chunk.getSource();
            RetrievedChunk existing = bestBySource.get(source);

            if (existing == null || c.boostedSim > existing.similarity) {
                // 🔥 문헌 text를 줄여서 저장 (중요!)
                String shortened = shorten(c.chunk.getText(), MAX_TEXT_LEN);

                bestBySource.put(source,
                        new RetrievedChunk(source, shortened, c.boostedSim));
            }
        }

        // 5. 문헌별 상위 1개 중 전체 TOP_K 선택
        List<RetrievedChunk> aggregated = new ArrayList<>(bestBySource.values());
        aggregated.sort((a, b) -> Double.compare(b.similarity, a.similarity));

        if (aggregated.size() > TOP_K) {
            aggregated = aggregated.subList(0, TOP_K);
        }

        // 5-1. 최종 선택된 문헌 로그 출력
        log.info("✅ [RAG] 최종 선택 문헌 (문헌별 상위 1개, 최대 {}개)", TOP_K);
        for (RetrievedChunk r : aggregated) {
            log.info("   ✔ src={} sim={} preview={}",
                    r.source,
                    String.format("%.4f", r.similarity),
                    preview(r.text, 80));
        }

        return aggregated;
    }


    /* ============================================================
     * 🔸 코사인 유사도 계산
     * ============================================================ */
    private double cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length == 0 || b.length == 0 || a.length != b.length)
            return 0.0;

        double dot = 0.0, normA = 0.0, normB = 0.0;

        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        if (normA == 0 || normB == 0) return 0.0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }


    /* ============================================================
     * 🔸 목표 기반 가중치 Boost
     * ============================================================ */
    private double applyGoalAwareBoost(String query, String source, double baseSim) {
        if (query == null || query.isBlank()) return baseSim;

        String q = query.toLowerCase();
        String s = source.toLowerCase();

        double boost = 0.0;

        // 감정 관련
        if (containsAny(q, "스트레스", "stress", "감정", "기분", "우울", "불안", "멘탈")) {
            if (s.contains("stress")) boost += 0.15;
        }

        // 운동 관련
        if (containsAny(q, "운동", "activity", "활동량", "유산소", "근력", "소모")) {
            if (s.contains("physical-activity")) boost += 0.10;
        }

        // 체중/비만 관련
        if (containsAny(q, "체중", "몸무게", "비만", "obesity", "감량")) {
            if (s.contains("obesity")) boost += 0.08;
        }

        // 영양/식단 관련
        if (containsAny(q, "칼로리", "섭취", "영양", "식단", "diet", "nutrition",
                "단백질", "탄수화물", "지방")) {
            if (s.contains("kdr") || s.contains("dietary"))
                boost += 0.06;
        }

        return baseSim + boost;
    }


    /* ============================================================
     * 🔸 여러 키워드 포함 여부
     * ============================================================ */
    private boolean containsAny(String text, String... keywords) {
        for (String k : keywords) {
            if (text.contains(k.toLowerCase())) return true;
        }
        return false;
    }


    /* ============================================================
     * 🔸 로그 출력용 Preview (문장 축소)
     * ============================================================ */
    private String preview(String text, int maxLen) {
        if (text == null) return "";
        text = text.replaceAll("\\s+", " ").trim();
        return (text.length() <= maxLen) ? text : text.substring(0, maxLen) + "...";
    }


    /* ============================================================
     * 🔸 실제 Gemini Prompt에 넣을 문헌 텍스트 축소
     * ============================================================ */
    public static String shorten(String text, int maxLen) {
        if (text == null) return "";
        text = text.replaceAll("\\s+", " ").trim();

        if (text.length() <= maxLen) return text;

        int cut = text.lastIndexOf(" ", maxLen);
        if (cut < 50) cut = maxLen;

        return text.substring(0, cut) + "…";
    }


    /* ============================================================
     * 🔸 내부 계산용 구조체
     * ============================================================ */
    private static class ChunkScore {
        final GuidelineChunk chunk;
        final double rawSim;
        final double boostedSim;

        ChunkScore(GuidelineChunk chunk, double rawSim, double boostedSim) {
            this.chunk = chunk;
            this.rawSim = rawSim;
            this.boostedSim = boostedSim;
        }
    }


    /* ============================================================
     * 🔸 최종 반환 DTO
     * ============================================================ */
    @RequiredArgsConstructor
    public static class RetrievedChunk {
        public final String source;
        public final String text;  // ← 🔥 shorten 적용된 텍스트
        public final double similarity;
    }
}
