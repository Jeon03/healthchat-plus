package com.healthchat.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * ✨ Google Gemini API 호출 유틸 (v2.5 대응 + 예외처리 안정형)
 */
@Component
@RequiredArgsConstructor
public class GeminiClient {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.model.default}")
    private String model;

    // ✅ WebClient를 외부 Bean에서 주입받음
    private final WebClient geminiWebClient;

    /**
     * Gemini API를 호출하여 JSON 텍스트 응답을 반환
     */
    public String generateJson(String prompt) {
        try {
            String endpoint = String.format("/%s:generateContent?key=%s", model, apiKey);

            Map<String, Object> body = Map.of(
                    "contents", List.of(
                            Map.of(
                                    "role", "user",
                                    "parts", List.of(Map.of("text", prompt))
                            )
                    )
            );

            Map<?, ?> response = geminiWebClient.post()
                    .uri(endpoint)
                    .bodyValue(body)
                    .retrieve()
                    // ✅ 명시적 람다 사용 (시그니처 호환 문제 해결)
                    .onStatus(status -> status.is5xxServerError(),
                            res -> Mono.error(new RuntimeException("Gemini 서버 오류 (5xx): " + res.statusCode())))
                    .onStatus(status -> status.is4xxClientError(),
                            res -> Mono.error(new RuntimeException("Gemini 요청 오류 (4xx): " + res.statusCode())))
                    .bodyToMono(Map.class)
                    .retryWhen(Retry.fixedDelay(1, Duration.ofSeconds(2))) // ✅ 1회 재시도
                    .block();

            return extractText(response);

        } catch (Exception e) {
            System.err.println("⚠️ Gemini API 호출 실패 (" + model + "): " + e.getMessage());
            return fallbackJson(); // ✅ fallback 최소화 (테스트용)
        }
    }

    /**
     * Gemini 응답 객체에서 text 내용만 추출
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
        String text = part.get("text").toString();

        System.out.println("✅ Gemini 응답 텍스트 추출 완료: " + text);
        return text;
    }

    /**
     * 임시 fallback JSON (테스트용)
     */
    private String fallbackJson() {
        return """
        {
          "meals": [
            {
              "time": "breakfast",
              "foods": [
                {"name": "rice", "quantity": 210, "unit": "g"}
              ]
            }
          ]
        }
        """;
    }
}
