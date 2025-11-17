package com.healthchat.backend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiClient {

    @Value("${gemini.model.flash}")
    private String flashModel;

    @Value("${gemini.model.pro}")
    private String proModel;

    @Value("${gemini.model.embed}")
    private String embedModel;

    @Value("${gemini.api.key}")
    private String apiKey;

    private final WebClient geminiWebClient;

    private static final int MAX_RETRIES = 5;
    private static final long BASE_DELAY_MS = 300L;


    /* ============================================================
     *  ⭐ pro → flash 자동 fallback 스마트 요청
     * ============================================================ */
    public String generateSmartJson(String prompt) {

        // 1) pro 모델 우선 요청
        String proResult = generateJson(proModel, prompt);

        if (proResult != null && !proResult.isBlank()) {
            log.info("✨ Gemini Smart: pro 모델 응답 성공");
            return proResult;
        }

        log.warn("⚠️ Gemini Smart: pro 실패 → flash fallback 실행");

        // 2) flash fallback
        String flashResult = generateJson(flashModel, prompt);

        if (flashResult != null && !flashResult.isBlank()) {
            log.info("✨ Gemini Smart: flash fallback 성공");
            return flashResult;
        }

        // 3) flash도 실패하면 빈 문자열 반환
        log.error("❌ Gemini Smart: flash까지 실패 → 최종 빈 응답 반환");
        return "";
    }


    /* ============================================================
     *  🔥 generateJson — 안정화 버전
     * ============================================================ */
    public String generateJson(String model, String prompt) {

        // prompt 길이 제한 — 너무 길면 모델이 silence
        if (prompt.length() > 6000) {
            prompt = prompt.substring(0, 6000) + "\n...(truncated)...";
        }

        for (int retry = 0; retry < MAX_RETRIES; retry++) {
            try {
                String endpoint = String.format("/%s:generateContent", model);

                Map<String, Object> body = Map.of(
                        "contents", List.of(
                                Map.of(
                                        "parts", List.of(
                                                Map.of("text", prompt)
                                        )
                                )
                        )
                );

                Map<?, ?> response = geminiWebClient.post()
                        .uri(uri -> uri
                                .path(endpoint)
                                .queryParam("key", apiKey)
                                .build())
                        .bodyValue(body)
                        .retrieve()
                        .onStatus(
                                status -> status.is4xxClientError(),
                                res -> Mono.error(new RuntimeException("Gemini 요청 오류(4xx): " + res.statusCode()))
                        )
                        .onStatus(
                                status -> status.is5xxServerError(),
                                res -> Mono.error(new RuntimeException("Gemini 서버 오류(5xx): " + res.statusCode()))
                        )
                        .bodyToMono(Map.class)
                        .timeout(Duration.ofSeconds(30))
                        .block();

                if (response != null) {
                    return extractText(response);
                }

                throw new RuntimeException("Gemini 응답 null");

            } catch (Exception e) {

                long delay = (long) (BASE_DELAY_MS * Math.pow(2, retry));

                log.warn("⚠️ Gemini retry {}/{} after {}ms — reason: {}",
                        retry + 1, MAX_RETRIES, delay, e.getMessage());

                try { Thread.sleep(delay); } catch (InterruptedException ignored) {}
            }
        }

        log.error("❌ Gemini generateJson 실패 — 모든 재시도 끝");
        return "";
    }

    /** flash 기본 */
    public String generateJson(String prompt) {
        return generateJson(flashModel, prompt);
    }


    /* ============================================================
     *  🔥 embed — 안정화 버전
     * ============================================================ */
    public float[] embed(String text) {

        if (text == null || text.isBlank()) {
            return new float[0];
        }

        if (text.length() > 3000) {
            text = text.substring(0, 3000);
        }

        for (int retry = 0; retry < MAX_RETRIES; retry++) {
            try {

                String endpoint = String.format("/%s:embedContent", embedModel);

                Map<String, Object> body = Map.of(
                        "model", embedModel,
                        "content", Map.of(
                                "parts", List.of(
                                        Map.of("text", text)
                                )
                        )
                );

                Map<?, ?> response = geminiWebClient.post()
                        .uri(uri -> uri
                                .path(endpoint)
                                .queryParam("key", apiKey)
                                .build())
                        .bodyValue(body)
                        .retrieve()
                        .onStatus(
                                status -> status.is4xxClientError(),
                                res -> Mono.error(new RuntimeException("Gemini 임베딩 오류(4xx): " + res.statusCode()))
                        )
                        .onStatus(
                                status -> status.is5xxServerError(),
                                res -> Mono.error(new RuntimeException("Gemini 임베딩 서버 오류(5xx): " + res.statusCode()))
                        )
                        .bodyToMono(Map.class)
                        .timeout(Duration.ofSeconds(30))
                        .block();

                if (response != null) {
                    return extractEmbedding(response);
                }

                throw new RuntimeException("Gemini 임베딩 응답 null");

            } catch (Exception e) {

                long delay = (long) (BASE_DELAY_MS * Math.pow(2, retry));

                log.warn("⚠️ Gemini embed retry {}/{} after {}ms — reason: {}",
                        retry + 1, MAX_RETRIES, delay, e.getMessage());

                try { Thread.sleep(delay); } catch (InterruptedException ignored) {}
            }
        }

        log.error("❌ Gemini embed 실패 — 모든 재시도 끝");
        return new float[0];
    }


    /* ============================================================
     *  🔍 응답 텍스트 추출
     * ============================================================ */
    private String extractText(Map<?, ?> response) {
        try {
            var candidates = (List<?>) response.get("candidates");
            if (candidates == null || candidates.isEmpty()) return "";

            Map<?, ?> first = (Map<?, ?>) candidates.get(0);
            Map<?, ?> content = (Map<?, ?>) first.get("content");
            if (content == null) return "";

            List<?> parts = (List<?>) content.get("parts");
            if (parts == null || parts.isEmpty()) return "";

            Map<?, ?> part = (Map<?, ?>) parts.get(0);

            return part.get("text") == null ? "" : part.get("text").toString();

        } catch (Exception e) {
            log.error("❌ extractText 오류: {}", e.getMessage());
            return "";
        }
    }


    /* ============================================================
     *  🔍 임베딩 추출
     * ============================================================ */
    private float[] extractEmbedding(Map<?, ?> response) {
        try {
            Map<?, ?> embedding = (Map<?, ?>) response.get("embedding");
            if (embedding == null) return new float[0];

            List<?> values = (List<?>) embedding.get("values");
            if (values == null) return new float[0];

            float[] vector = new float[values.size()];
            for (int i = 0; i < values.size(); i++) {
                vector[i] = ((Number) values.get(i)).floatValue();
            }
            return vector;

        } catch (Exception e) {
            log.error("❌ extractEmbedding 오류: {}", e.getMessage());
            return new float[0];
        }
    }
}
