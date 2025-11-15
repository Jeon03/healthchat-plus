package com.healthchat.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class GeminiClient {

    @Value("${gemini.model.flash}")
    private String flashModel;

    @Value("${gemini.model.pro}")
    private String proModel;

    @Value("${gemini.model.embed}")
    private String embedModel;  // ⭐ 임베딩 모델 추가

    @Value("${gemini.api.key}")
    private String apiKey;      // ⭐ API KEY 주입

    private final WebClient geminiWebClient;

    /**
     * ====================================================
     *  🔥 1) 텍스트 생성(generateContent)
     * ====================================================
     */
    public String generateJson(String model, String prompt) {
        try {
            // 엔드포인트: /v1beta/models/{model}:generateContent?key=API_KEY
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
                    .uri(uriBuilder -> uriBuilder
                            .path(endpoint)
                            .queryParam("key", apiKey)
                            .build())
                    .bodyValue(body)
                    .retrieve()
                    .onStatus(
                            status -> status.is4xxClientError(),
                            res -> Mono.error(
                                    new RuntimeException("Gemini 요청 오류 (4xx): " + res.statusCode())
                            )
                    )
                    .onStatus(
                            status -> status.is5xxServerError(),
                            res -> Mono.error(
                                    new RuntimeException("Gemini 서버 오류 (5xx): " + res.statusCode())
                            )
                    )
                    .bodyToMono(Map.class)
                    .retryWhen(Retry.fixedDelay(1, Duration.ofSeconds(1)))
                    .block();

            return extractText(response);

        } catch (Exception e) {
            System.err.println("⚠️ Gemini API 호출 실패 (" + model + "): " + e.getMessage());
            return null;
        }
    }

    /** 기본 모델 flash 사용 */
    public String generateJson(String prompt) {
        return generateJson(flashModel, prompt);
    }

    /**
     * ====================================================
     *  🔥 2) 텍스트 임베딩(embedContent)
     * ====================================================
     */
    public float[] embed(String text) {
        try {
            // 엔드포인트: /v1beta/models/gemini-embedding-001:embedContent
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
                    .uri(uriBuilder -> uriBuilder
                            .path(endpoint)
                            .queryParam("key", apiKey)
                            .build())
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .retryWhen(Retry.fixedDelay(1, Duration.ofSeconds(1)))
                    .block();

            return extractEmbedding(response);

        } catch (Exception e) {
            System.err.println("⚠️ Gemini 임베딩 실패: " + e.getMessage());
            return new float[0];
        }
    }

    /**
     * ====================================================
     *  🔍 응답에서 텍스트 추출
     * ====================================================
     */
    private String extractText(Map<?, ?> response) {
        if (response == null)
            throw new RuntimeException("Gemini 응답이 null입니다.");

        var candidates = (List<?>) response.get("candidates");
        if (candidates == null || candidates.isEmpty())
            throw new RuntimeException("Gemini 응답이 비어 있음");

        Map<?, ?> first = (Map<?, ?>) candidates.get(0);
        Map<?, ?> content = (Map<?, ?>) first.get("content");

        List<?> parts = (List<?>) content.get("parts");
        if (parts == null || parts.isEmpty())
            throw new RuntimeException("Gemini parts가 비어 있음");

        Map<?, ?> part = (Map<?, ?>) parts.get(0);

        return part.get("text").toString();
    }


    /**
     * ====================================================
     *  🔍 응답에서 임베딩 벡터 추출
     * ====================================================
     */
    private float[] extractEmbedding(Map<?, ?> response) {
        if (response == null)
            throw new RuntimeException("Gemini 임베딩 응답이 null입니다.");

        Map<?, ?> embedding = (Map<?, ?>) response.get("embedding");
        if (embedding == null)
            throw new RuntimeException("embedding 필드 없음");

        List<?> values = (List<?>) embedding.get("values");
        if (values == null)
            throw new RuntimeException("values 필드 없음");

        float[] vector = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            vector[i] = ((Number) values.get(i)).floatValue();
        }

        return vector;
    }
}
