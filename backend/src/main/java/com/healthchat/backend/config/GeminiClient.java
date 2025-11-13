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

    private final WebClient geminiWebClient;

    /**
     * 🔥 모델을 지정해서 JSON 생성 요청
     */
    public String generateJson(String model, String prompt) {
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
                    .uri(endpoint)
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

    /**
     * 🔥 기존 flash 모델을 기본으로 사용하는 메서드 (기존 코드 호환용)
     */
    public String generateJson(String prompt) {
        return generateJson(flashModel, prompt);
    }

    /**
     * 🔍 Gemini 응답에서 텍스트 부분 추출
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
}
