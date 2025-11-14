package com.healthchat.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthchat.backend.config.GeminiClient;
import com.healthchat.backend.dto.EmotionAnalysisResult;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class GeminiEmotionAnalysisService {

    private final GeminiClient geminiClient;
    private final ObjectMapper mapper = new ObjectMapper();

    @Async
    public CompletableFuture<EmotionAnalysisResult> analyzeEmotion(String text) {

        long start = System.currentTimeMillis();

        if (text == null || text.isBlank()) {
            return CompletableFuture.completedFuture(
                    EmotionAnalysisResult.builder()
                            .action("none")
                            .emotions(List.of())
                            .scores(List.of())
                            .summaries(List.of())
                            .keywords(List.of())
                            .primaryEmotion("")
                            .primaryScore(0)
                            .rawText(text)
                            .build()
            );
        }

        String prompt = buildPrompt(text);
        String response = geminiClient.generateJson("gemini-2.5-pro", prompt);
        String json = extractJson(response);

        try {
            Map<String, Object> map = mapper.readValue(json, Map.class);

            List<String> emotions = safeStringList(map.get("emotions"));
            List<Integer> scores = safeIntList(map.get("scores"));
            List<String> summaries = safeStringList(map.get("summaries"));
            List<List<String>> keywords = safeDoubleStringList(map.get("keywords"));

            String primaryEmotion = map.getOrDefault("primaryEmotion", "").toString();
            int primaryScore = safeInt(map.get("primaryScore"));

            String action = detectAction(text, emotions);

            EmotionAnalysisResult result = EmotionAnalysisResult.builder()
                    .action(action)
                    .emotions(emotions)
                    .scores(scores)
                    .summaries(summaries)
                    .keywords(keywords)
                    .primaryEmotion(primaryEmotion)
                    .primaryScore(primaryScore)
                    .rawText(text)
                    .build();

            long took = System.currentTimeMillis() - start;
            System.out.printf(
                    "✅ [Emotion] 감정 분석 완료 → %s | 대표:%s (%d점), 감정 %d개 — %dms%n",
                    action, primaryEmotion, primaryScore, emotions.size(), took
            );
            return CompletableFuture.completedFuture(result);

        } catch (Exception e) {
            System.err.println("⚠ Emotion JSON parsing failed: " + e.getMessage());
            System.err.println("원본 응답: " + response);

            EmotionAnalysisResult fallback = EmotionAnalysisResult.builder()
                    .action("replace")
                    .emotions(List.of())
                    .scores(List.of())
                    .summaries(List.of())
                    .keywords(List.of())
                    .primaryEmotion("")
                    .primaryScore(0)
                    .rawText(text)
                    .build();

            return CompletableFuture.completedFuture(fallback);
        }
    }


    private String buildPrompt(String text) {
        return """
한국어 문장에서 **여러 감정**을 모두 분석하여 아래 JSON 형식으로만 반환하라.
설명, 말머리, 코드블록, 사족 절대 금지. JSON만 출력.

{
  "emotions": [],          // 예: ["우울", "기쁨"]
  "scores": [],            // 감정 강도 (0~100)
  "summaries": [],         // 각 감정의 한줄 요약
  "keywords": [],          // 각 감정의 핵심 명사 리스트 (2차원 배열)
  "primaryEmotion": "",    // 강도(score)가 가장 높은 감정
  "primaryScore": 0
}

규칙:

[1] 감정 추출
- 문장에서 감정(우울, 기쁨, 스트레스, 짜증, 피곤 등)을 모두 찾아 순서대로 기록한다.
- 감정이 여러 번 변하면 그 순서 그대로 emotions 배열에 기록한다.

[2] 점수
- scores[i] 는 emotions[i]의 강도이다.
- 점수 범위: 0~100

[3] summaries
- summaries[i] 는 emotions[i]의 상황 요약(짧은 한 문장)이다.

[4] keywords (중요)
- keywords[i]는 **emotions[i]을 유발한 핵심 요인 단어들(명사)**이며 반드시 다음 규칙을 따라야 한다.
    - 반드시 명사 기반 단어로만 구성한다.
    - 조사, 어미, 접속어 제거 (예: "우울했는데" → "우울")
    - 문장 조각, 구절, 동사, 종결형 금지.
    - 예시는 다음과 같다:
        "피곤하고 우울했는데"  → ["피곤", "우울"]
        "친구랑 얘기하니까 기분 좋아졌어" → ["친구", "대화", "기분전환"]
        "과제 때문에 스트레스 받았어" → ["과제", "스트레스"]
    - 키워드는 각 감정당 1~3개만.

[5] primaryEmotion / primaryScore
- scores 배열에서 가장 높은 점수를 가진 감정을 primaryEmotion으로 설정한다.

반환 형식:
- JSON만 출력. 설명 절대 포함 금지.

분석 대상 문장:
"%s"
""".formatted(text);
    }

    /* ==========================================================================
       JSON만 추출
     ========================================================================== */
    private String extractJson(String text) {
        if (text == null) return "{}";
        int s = text.indexOf("{");
        int e = text.lastIndexOf("}");
        if (s >= 0 && e > s) return text.substring(s, e + 1);
        return "{}";
    }

    /* ==========================================================================
       안전 파싱 함수들
     ========================================================================== */
    private int safeInt(Object obj) {
        try { return Integer.parseInt(obj.toString()); }
        catch (Exception e) { return 0; }
    }

    private List<String> safeStringList(Object obj) {
        try { return (List<String>) obj; }
        catch (Exception e) { return List.of(); }
    }

    private List<Integer> safeIntList(Object obj) {
        try { return (List<Integer>) obj; }
        catch (Exception e) { return List.of(); }
    }

    private List<List<String>> safeDoubleStringList(Object obj) {
        try { return (List<List<String>>) obj; }
        catch (Exception e) { return List.of(); }
    }

    /* ==========================================================================
       action 자동 감지 (삭제/수정/추가)
     ========================================================================== */
    private String detectAction(String text, List<String> emotions) {

        String lower = text.toLowerCase();

        // 삭제 요청 탐지
        if (lower.contains("지워") || lower.contains("삭제") || lower.contains("없애")) {
            return "delete";
        }

        // 수정 요청 탐지
        if (lower.contains("다시") || lower.contains("수정") || lower.contains("바꿔")) {
            return "update";
        }

        // 감정 하나도 없으면 replace
        if (emotions.isEmpty()) return "replace";

        // 새 감정 입력 → replace(덮어쓰기)
        return "replace";
    }
}
