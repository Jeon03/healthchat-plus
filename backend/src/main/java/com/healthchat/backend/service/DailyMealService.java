package com.healthchat.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthchat.backend.dto.DailyAnalysis;
import com.healthchat.backend.dto.FoodItem;
import com.healthchat.backend.dto.MealEntry;
import com.healthchat.backend.entity.DailyMeal;
import com.healthchat.backend.entity.User;
import com.healthchat.backend.repository.DailyMealRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DailyMealService {

    private final DailyMealRepository dailyMealRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();


    /** ✅ 오늘 식단 조회 */
    public DailyMeal getTodayMeal(User user) {
        LocalDate today = LocalDate.now();
        return dailyMealRepository.findByUserIdAndDate(user.getId(), today).orElse(null);
    }

    /** ✅ 특정 날짜 식단 조회 (옵션) */
    public DailyMeal getMealByDate(User user, LocalDate date) {
        return dailyMealRepository.findByUserIdAndDate(user.getId(), date).orElse(null);
    }


    public DailyMeal saveDailyMeal(User user, DailyAnalysis analysis) {
        LocalDate today = LocalDate.now();

        // ✅ 오늘 날짜의 기존 식단 기록 조회 (없으면 새로 생성)
        DailyMeal meal = dailyMealRepository.findByUserIdAndDate(user.getId(), today)
                .orElse(DailyMeal.builder()
                        .user(user)
                        .date(today)
                        .totalCalories(0.0)
                        .totalProtein(0.0)
                        .totalFat(0.0)
                        .totalCarbs(0.0)
                        .build());

        // ✅ 기존 식단 파싱
        List<MealEntry> meals = new ArrayList<>();
        if (meal.getMealsJson() != null && !meal.getMealsJson().isBlank()) {
            try {
                meals = objectMapper.readValue(meal.getMealsJson(), new TypeReference<>() {});
            } catch (Exception e) {
                System.err.println("⚠️ 기존 식단 JSON 파싱 실패: " + e.getMessage());
            }
        }

        // ✅ action에 따라 분기
        String action = analysis.getAction() == null ? "add" : analysis.getAction();
        String target = analysis.getTargetMeal();

        switch (action) {
            case "update" -> {
                System.out.println("✏️ 식단 수정 감지 → " + target);
                if (target != null) {
                    // 같은 끼니 제거 후 새 식단 추가
                    meals.removeIf(m -> m.getTime().equals(target));
                }
                meals.addAll(analysis.getMeals());
            }
            case "delete" -> {
                System.out.println("🗑️ 식단 삭제 감지 → " + target);
                if (target != null) {
                    meals.removeIf(m -> m.getTime().equals(target));
                }
            }
            default -> { // add
                System.out.println("➕ 식단 추가 감지");
                meals.addAll(analysis.getMeals());
            }
        }

// ✅ 안전한 합계 계산
        double totalKcal = 0, totalProtein = 0, totalFat = 0, totalCarbs = 0;
        for (MealEntry m : meals) {
            for (FoodItem f : m.getFoods()) {
                totalKcal += f.getCalories() != null ? f.getCalories() : 0;
                totalProtein += f.getProtein() != null ? f.getProtein() : 0;
                totalFat += f.getFat() != null ? f.getFat() : 0;
                totalCarbs += f.getCarbs() != null ? f.getCarbs() : 0;
            }
        }

        // ✅ 직렬화 후 저장
        try {
            meal.setMealsJson(objectMapper.writeValueAsString(meals));
        } catch (Exception e) {
            throw new RuntimeException("식단 JSON 직렬화 실패", e);
        }

        meal.setTotalCalories(totalKcal);
        meal.setTotalProtein(totalProtein);
        meal.setTotalFat(totalFat);
        meal.setTotalCarbs(totalCarbs);

        DailyMeal saved = dailyMealRepository.save(meal);

        System.out.printf("✅ [%s] 처리 완료 (user:%d / %s)%n", action, user.getId(), today);
        System.out.printf("총합 → %.1f kcal | P: %.1f | F: %.1f | C: %.1f%n",
                totalKcal, totalProtein, totalFat, totalCarbs);

        return saved;
    }

    @Transactional
    public DailyMeal saveOrUpdateManual(User user, DailyMeal updated) {
        // ✅ date 필드가 LocalDate라면 parse() 불필요
        LocalDate date = updated.getDate() != null ? updated.getDate() : LocalDate.now();

        // ✅ 기존 데이터 조회 (있으면 수정, 없으면 새로 생성)
        DailyMeal meal = dailyMealRepository.findByUserIdAndDate(user.getId(), date)
                .orElse(DailyMeal.builder()
                        .user(user)
                        .date(date)
                        .build());

        // ✅ 필드 교체
        meal.setMealsJson(updated.getMealsJson());
        meal.setTotalCalories(updated.getTotalCalories());
        meal.setTotalProtein(updated.getTotalProtein());
        meal.setTotalFat(updated.getTotalFat());
        meal.setTotalCarbs(updated.getTotalCarbs());

        return dailyMealRepository.save(meal);
    }

}
