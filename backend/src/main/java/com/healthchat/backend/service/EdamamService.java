package com.healthchat.backend.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EdamamService {

    @Value("${edamam.app-id}")
    private String appId;

    @Value("${edamam.app-key}")
    private String appKey;

    private final WebClient webClient = WebClient.create("https://api.edamam.com/api/nutrition-data");

    @PostConstruct
    public void debugKeys() {
        System.out.println("✅ Edamam Config Loaded → appId=" + appId + ", appKey=" + appKey);
    }

    /**
     * ✅ 음식명 기반 영양정보 전체 반환 + 칼로리/탄단지 포함
     */
    public Map<String, Object> getNutrition(String food) {
        try {
            System.out.println("📤 [Edamam 요청] ingr=" + food);

            Map<String, Object> response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("app_id", appId)
                            .queryParam("app_key", appKey)
                            .queryParam("ingr", food)
                            .build())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            System.out.println("📥 [Edamam 응답] " + response);

            // ✅ 주요 영양 성분 추출
            Map<String, Double> nutrients = extractNutrients(response);

            System.out.printf(
                    "🍱 [영양요약] %s → %.1f kcal | 단백질 %.1fg | 지방 %.1fg | 탄수화물 %.1fg%n",
                    food,
                    nutrients.get("calories"),
                    nutrients.get("protein"),
                    nutrients.get("fat"),
                    nutrients.get("carbs")
            );

            // 🔹 응답 Map에 요약 데이터 추가
            response.put("totalCalories", nutrients.get("calories"));
            response.put("totalProtein", nutrients.get("protein"));
            response.put("totalFat", nutrients.get("fat"));
            response.put("totalCarbs", nutrients.get("carbs"));

            return response;

        } catch (Exception e) {
            throw new RuntimeException("❌ Edamam API 호출 실패: " + e.getMessage());
        }
    }

    /**
     * ✅ Edamam 응답에서 ENERC_KCAL, PROCNT, FAT, CHOCDF 추출
     */
    private Map<String, Double> extractNutrients(Map<String, Object> response) {
        Map<String, Double> result = new HashMap<>(Map.of(
                "calories", 0.0,
                "protein", 0.0,
                "fat", 0.0,
                "carbs", 0.0
        ));

        try {
            var ingredients = (List<?>) response.get("ingredients");
            if (ingredients == null || ingredients.isEmpty()) return result;

            Map<?, ?> firstIngredient = (Map<?, ?>) ingredients.get(0);
            var parsed = (List<?>) firstIngredient.get("parsed");
            if (parsed == null || parsed.isEmpty()) return result;

            Map<?, ?> firstParsed = (Map<?, ?>) parsed.get(0);
            Map<?, ?> nutrients = (Map<?, ?>) firstParsed.get("nutrients");
            if (nutrients == null) return result;

            result.put("calories", extractValue(nutrients, "ENERC_KCAL"));
            result.put("protein", extractValue(nutrients, "PROCNT"));
            result.put("fat", extractValue(nutrients, "FAT"));
            result.put("carbs", extractValue(nutrients, "CHOCDF"));

        } catch (Exception e) {
            System.out.println("⚠️ Edamam 영양 추출 실패: " + e.getMessage());
        }
        return result;
    }

    /**
     * ✅ 안전한 영양소 값 추출
     */
    private double extractValue(Map<?, ?> nutrients, String key) {
        try {
            Map<?, ?> nutrient = (Map<?, ?>) nutrients.get(key);
            if (nutrient == null) return 0.0;
            return ((Number) nutrient.get("quantity")).doubleValue();
        } catch (Exception e) {
            return 0.0;
        }
    }
}
