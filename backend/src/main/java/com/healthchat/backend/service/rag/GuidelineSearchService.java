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

    private final int TOP_K = 5;

    /**
     * 사용자 질문 → 임베딩 → 청크 검색 → 상위 K개 반환
     */
    public List<RetrievedChunk> searchRelevantChunks(String query) {

        // 1. query embedding 생성
        float[] queryEmbedding = gemini.embed(query);

        // 2. 모든 저장된 chunks 가져오기
        List<GuidelineChunk> all = repo.findAll();

        List<RetrievedChunk> results = new ArrayList<>();

        // 3. 각 chunk와 cosine similarity 계산
        for (GuidelineChunk chunk : all) {
            float[] chunkVector = EmbeddingUtil.toFloatArray(chunk.getEmbedding());

            double similarity = cosineSimilarity(queryEmbedding, chunkVector);

            results.add(new RetrievedChunk(chunk.getSource(), chunk.getText(), similarity));
        }

        // 4. similarity 기준 정렬
        results.sort((a, b) -> Double.compare(b.similarity, a.similarity));

        // 5. top K 반환
        return results.subList(0, Math.min(TOP_K, results.size()));
    }


    /**
     * 코사인 유사도 계산
     */
    private double cosineSimilarity(float[] a, float[] b) {

        // ❗ 추가: 벡터가 비었으면 similarity = 0으로 처리
        if (a == null || b == null || a.length == 0 || b.length == 0) {
            return 0.0;
        }

        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }


    /**
     * 검색 결과 DTO
     */
    @RequiredArgsConstructor
    public static class RetrievedChunk {
        public final String source;
        public final String text;
        public final double similarity;
    }
}
