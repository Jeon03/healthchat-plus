package com.healthchat.backend.service;

import com.healthchat.backend.dto.DailyAnalysis;
import com.healthchat.backend.dto.FoodItem;
import com.healthchat.backend.dto.MealEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DailyNutritionService {

    private final EdamamService edamamService;

    public DailyAnalysis processMeals(DailyAnalysis analysis) {
        double totalCalories = 0;
        double totalProtein = 0;
        double totalFat = 0;
        double totalCarbs = 0;

        if (analysis.getMeals() == null) {
            System.out.println("⚠️ Gemini 결과에 식단 데이터가 없습니다.");
            return analysis;
        }

        for (MealEntry meal : analysis.getMeals()) {
            if (meal.getFoods() == null) continue;

            for (FoodItem food : meal.getFoods()) {
                // ✅ 1️⃣ 음식 이름 자동 보정
                String normalizedName = normalizeFoodName(food.getName());
                String adjustedQuery = buildQuery(normalizedName, food.getQuantity(), food.getUnit());

                try {
                    Map<String, Object> res = edamamService.getNutrition(adjustedQuery);

                    // ✅ 2️⃣ Edamam 응답 파싱
                    List<Map<String, Object>> ingredients = (List<Map<String, Object>>) res.get("ingredients");
                    if (ingredients == null || ingredients.isEmpty()) continue;

                    List<Map<String, Object>> parsedList =
                            (List<Map<String, Object>>) ingredients.get(0).get("parsed");
                    if (parsedList == null || parsedList.isEmpty()) continue;

                    Map<String, Map<String, Object>> nutrients =
                            (Map<String, Map<String, Object>>) parsedList.get(0).get("nutrients");
                    if (nutrients == null) continue;

                    // ✅ 3️⃣ 주요 영양소 추출
                    double kcal = getQuantity(nutrients, "ENERC_KCAL");
                    double protein = getQuantity(nutrients, "PROCNT");
                    double fat = getQuantity(nutrients, "FAT");
                    double carbs = getQuantity(nutrients, "CHOCDF");

                    // ✅ 4️⃣ kcal 상한 제한 (예: 1식품 1000kcal 초과 방지)
                    if (kcal > 1000) kcal = 1000;

                    // ✅ 5️⃣ 개별 음식에 반영
                    food.setName(normalizedName);
                    food.setCalories(kcal);
                    food.setProtein(protein);
                    food.setFat(fat);
                    food.setCarbs(carbs);

                    // ✅ 합산
                    totalCalories += kcal;
                    totalProtein += protein;
                    totalFat += fat;
                    totalCarbs += carbs;

                    System.out.printf(
                            "🍱 [영양요약] %s → %.1f kcal | P: %.1fg | F: %.1fg | C: %.1fg%n",
                            adjustedQuery, kcal, protein, fat, carbs
                    );

                } catch (Exception e) {
                    System.err.println("❌ Edamam 처리 실패 (" + adjustedQuery + "): " + e.getMessage());
                }
            }
        }

        // ✅ 6️⃣ 총합을 analysis에 반영
        analysis.setTotalCalories(totalCalories);
        analysis.setTotalProtein(totalProtein);
        analysis.setTotalFat(totalFat);
        analysis.setTotalCarbs(totalCarbs);

        System.out.printf("✅ 총합 → %.1f kcal | P: %.1fg | F: %.1fg | C: %.1fg%n",
                totalCalories, totalProtein, totalFat, totalCarbs);

        return analysis;
    }

    /** ✅ 단위 보정 (serving 단위로 변환) */
    private String buildQuery(String foodName, double qty, String unit) {
        // Gemini가 이미 gram 단위로 준 경우 그대로 사용
        if (unit.equalsIgnoreCase("g") || unit.equalsIgnoreCase("gram")) {
            return qty + " g " + foodName;
        }

        // 한국어에서 '개', '공기' 등일 때만 serving 변환
        if (unit.equalsIgnoreCase("bowl") || unit.equalsIgnoreCase("serving") || unit.equalsIgnoreCase("개")) {
            return "1 serving " + foodName;
        }

        // 기본값
        return qty + " " + unit + " " + foodName;
    }

    /** ✅ 한글 음식명 → 영어 변환 */
    private String normalizeFoodName(String name) {
        return switch (name.toLowerCase()) {
            case "라면" -> "ramen";
            case "밥", "흰쌀밥" -> "cooked rice";
            case "김치" -> "kimchi";
            case "사과" -> "apple";
            case "빵" -> "bread";
            case "우유" -> "milk";
            default -> name;
        };
    }

    private double getQuantity(Map<String, Map<String, Object>> map, String key) {
        if (map != null && map.containsKey(key)) {
            Object q = map.get(key).get("quantity");
            if (q instanceof Number) return ((Number) q).doubleValue();
        }
        return 0.0;
    }
}
