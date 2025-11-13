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

    /** ✅ 특정 날짜 식단 조회 */
    public DailyMeal getMealByDate(User user, LocalDate date) {
        return dailyMealRepository.findByUserIdAndDate(user.getId(), date).orElse(null);
    }

    @Transactional
    public DailyMeal saveDailyMeal(User user, DailyAnalysis analysis) {
        LocalDate today = LocalDate.now();

        // ✅ 오늘 기록 조회 (없으면 새로 생성)
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

        // ✅ action / target 정리
        String action = analysis.getAction() == null ? "add" : analysis.getAction();
        String target = analysis.getTargetMeal();
        List<MealEntry> newMeals = analysis.getMeals() != null ? analysis.getMeals() : List.of();

        System.out.printf("📌 DailyMealService - action=%s, target=%s, newMeals=%d개%n",
                action, target, newMeals.size());

        // ✅ [보정] update인데 targetMeal이 없고, 끼니가 1개만 있으면 → 그 끼니를 target으로 사용
        if ("update".equals(action) && target == null && newMeals.size() == 1) {
            target = newMeals.get(0).getTime();
            System.out.println("🔧 targetMeal 자동 보정 → " + target);
        }

        switch (action) {
            case "replace" -> {
                // 🔄 전체 초기화 후 새 식단으로 교체
                System.out.println("🔁 전체 식단 교체 (replace)");
                meals.clear();
                meals.addAll(newMeals);
            }
            case "update" -> {
                System.out.println("✏️ 식단 수정 감지 → " + target);

                if (!newMeals.isEmpty()) {
                    if (target != null) {
                        // 🎯 "아침만 바꿔줘" 같은 경우 → 해당 끼니만 삭제 후 새로 추가
                        String finalTarget = target;
                        meals.removeIf(m -> finalTarget.equals(m.getTime()));
                    } else {
                        // 🧠 "아침은 ~~ 점심은 ~~ 저녁은 ~~" 같이 여러 끼니 한 번에 수정
                        //    → 새로 들어온 끼니들의 time 들을 기준으로 기존 것들 제거
                        var timesToReplace = newMeals.stream()
                                .map(MealEntry::getTime)
                                .filter(t -> t != null)
                                .collect(java.util.stream.Collectors.toSet());

                        meals.removeIf(m -> timesToReplace.contains(m.getTime()));
                        System.out.println("🔄 멀티 끼니 수정 → " + timesToReplace + " 교체");
                    }

                    // ✅ 새 분석 결과 추가
                    meals.addAll(newMeals);
                }
            }
            case "delete" -> {
                System.out.println("🗑️ 식단 삭제 감지 → " + target);
                if (target != null) {
                    String finalTarget = target;
                    meals.removeIf(m -> finalTarget.equals(m.getTime()));
                } else if (!newMeals.isEmpty()) {
                    // 예: "아침이랑 점심 빼줘" 같이 올 수도 있어서, newMeals 기준 제거
                    var timesToDelete = newMeals.stream()
                            .map(MealEntry::getTime)
                            .filter(t -> t != null)
                            .collect(java.util.stream.Collectors.toSet());
                    meals.removeIf(m -> timesToDelete.contains(m.getTime()));
                }
            }
            default -> {
                // ➕ 기본은 "추가" — 다만 여기선 그대로 addAll 유지
                //    (사용자가 "그리고 콜라 한 캔 더" 같은 걸 의도할 수 있어서)
                System.out.println("➕ 식단 추가 감지 (add)");
                meals.addAll(newMeals);
            }
        }

        // ✅ 총합 재계산
        double totalKcal = 0, totalProtein = 0, totalFat = 0, totalCarbs = 0;
        for (MealEntry m : meals) {
            if (m.getFoods() == null) continue;
            for (FoodItem f : m.getFoods()) {
                if (f == null) continue;
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
                totalKcal, totalProtein, totalCarbs, totalCarbs);

        return dailyMealRepository.findByUserIdAndDate(user.getId(), today)
                .orElse(saved);
    }

    @Transactional
    public DailyMeal saveOrUpdateManual(User user, DailyMeal updated) {
        LocalDate date = updated.getDate() != null ? updated.getDate() : LocalDate.now();

        DailyMeal meal = dailyMealRepository.findByUserIdAndDate(user.getId(), date)
                .orElse(DailyMeal.builder()
                        .user(user)
                        .date(date)
                        .build());

        meal.setMealsJson(updated.getMealsJson());
        meal.setTotalCalories(updated.getTotalCalories());
        meal.setTotalProtein(updated.getTotalProtein());
        meal.setTotalFat(updated.getTotalFat());
        meal.setTotalCarbs(updated.getTotalCarbs());

        return dailyMealRepository.save(meal);
    }
}

