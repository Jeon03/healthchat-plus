package com.healthchat.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthchat.backend.dto.DailyAnalysis;
import com.healthchat.backend.dto.FoodItem;
import com.healthchat.backend.entity.DailyMeal;
import com.healthchat.backend.entity.User;
import com.healthchat.backend.repository.DailyMealRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DailyMealService {

    private final DailyMealRepository dailyMealRepository;
    private final EdamamService edamamService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DailyMeal saveDailyMeal(User user, DailyAnalysis analysis) {
        double totalKcal = 0;
        double totalProtein = 0;
        double totalFat = 0;
        double totalCarbs = 0;

        for (var meal : analysis.getMeals()) {
            for (FoodItem food : meal.getFoods()) {
                String query = food.getQuantity() + " " + food.getUnit() + " " + food.getName();
                Map<String, Object> res = edamamService.getNutrition(query);

                try {
                    // ✅ nutrients 파싱
                    List<Map<String, Object>> ingredients = (List<Map<String, Object>>) res.get("ingredients");
                    if (ingredients == null || ingredients.isEmpty()) continue;

                    List<Map<String, Object>> parsedList =
                            (List<Map<String, Object>>) ingredients.get(0).get("parsed");
                    if (parsedList == null || parsedList.isEmpty()) continue;

                    Map<String, Map<String, Object>> nutrients =
                            (Map<String, Map<String, Object>>) parsedList.get(0).get("nutrients");
                    if (nutrients == null) continue;

                    double kcal = getQuantity(nutrients, "ENERC_KCAL");
                    double protein = getQuantity(nutrients, "PROCNT");
                    double fat = getQuantity(nutrients, "FAT");
                    double carbs = getQuantity(nutrients, "CHOCDF");

                    // ✅ 개별 음식 정보에 반영
                    food.setCalories(kcal);
                    food.setProtein(protein);
                    food.setFat(fat);
                    food.setCarbs(carbs);

                    System.out.printf("🍱 [영양요약] %s → %.1f kcal | 단백질 %.1fg | 지방 %.1fg | 탄수화물 %.1fg%n",
                            query, kcal, protein, fat, carbs);

                    // ✅ 총합 누적
                    totalKcal += kcal;
                    totalProtein += protein;
                    totalFat += fat;
                    totalCarbs += carbs;

                } catch (Exception e) {
                    System.err.println("❌ Edamam 파싱 실패 (" + query + "): " + e.getMessage());
                }
            }
        }

        // ✅ 합계 출력
        System.out.printf("✅ 총합 → %.1f kcal | 단백질 %.1fg | 지방 %.1fg | 탄수화물 %.1fg%n",
                totalKcal, totalProtein, totalFat, totalCarbs);

        // ✅ DB 저장
        String mealsJson;
        try {
            mealsJson = objectMapper.writeValueAsString(analysis.getMeals());
        } catch (Exception e) {
            throw new RuntimeException("식단 JSON 직렬화 실패", e);
        }

        LocalDate today = LocalDate.now();
        DailyMeal meal = dailyMealRepository.findByUserIdAndDate(user.getId(), today)
                .orElse(DailyMeal.builder()
                        .user(user)
                        .date(today)
                        .build());

        meal.setMealsJson(mealsJson);
        meal.setTotalCalories(totalKcal);
        meal.setTotalProtein(totalProtein);
        meal.setTotalFat(totalFat);
        meal.setTotalCarbs(totalCarbs);

        DailyMeal saved = dailyMealRepository.save(meal);
        System.out.printf("✅ DailyMeal 저장 완료 (user:%d / %s) → %.1f kcal%n",
                user.getId(), today, totalKcal);

        return saved;
    }

    private double getQuantity(Map<String, Map<String, Object>> map, String key) {
        if (map != null && map.containsKey(key)) {
            Object q = map.get(key).get("quantity");
            if (q instanceof Number) return ((Number) q).doubleValue();
        }
        return 0.0;
    }
}
