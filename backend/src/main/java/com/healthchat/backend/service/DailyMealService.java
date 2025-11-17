package com.healthchat.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthchat.backend.dto.DailyAnalysis;
import com.healthchat.backend.dto.FoodItem;
import com.healthchat.backend.dto.MealEntry;
import com.healthchat.backend.entity.DailyMeal;
import com.healthchat.backend.entity.User;
import com.healthchat.backend.repository.DailyLogRepository;
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

    private final DailyLogService dailyLogService;
    private final DailyMealRepository dailyMealRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public void deleteToday(User user) {
        LocalDate today = LocalDate.now();

        // 1️⃣ DailyLog에서 FK 먼저 끊기
        dailyLogService.clearMeal(user, today);

        // 2️⃣ 실제 DailyMeal 삭제
        dailyMealRepository.deleteByUserAndDate(user, today);

        System.out.println("🗑 식단 전체 삭제 완료");
    }

    /** ✅ 오늘 식단 조회 (안전 버전) */
    public DailyMeal getTodayMeal(User user) {
        LocalDate today = LocalDate.now();

        DailyMeal meal = dailyMealRepository.findByUserIdAndDate(user.getId(), today)
                .orElse(null);

        if (meal == null) {
            // 프론트에서 다루기 편하게 "빈 구조"로 반환
            return DailyMeal.builder()
                    .user(user)
                    .date(today)
                    .mealsJson("[]")
                    .totalCalories(0.0)
                    .totalProtein(0.0)
                    .totalFat(0.0)
                    .totalCarbs(0.0)
                    .build();
        }

        // JSON / 합계 값이 null인 경우 방어 코드
        if (meal.getMealsJson() == null || meal.getMealsJson().isBlank()) {
            meal.setMealsJson("[]");
        }
        if (meal.getTotalCalories() == null) meal.setTotalCalories(0.0);
        if (meal.getTotalProtein() == null) meal.setTotalProtein(0.0);
        if (meal.getTotalFat() == null) meal.setTotalFat(0.0);
        if (meal.getTotalCarbs() == null) meal.setTotalCarbs(0.0);

        return meal;
    }

    /** ✅ 특정 날짜 식단 조회 (필요하면 위처럼 안전 버전으로 변경 가능) */
    public DailyMeal getMealByDate(User user, LocalDate date) {
        return dailyMealRepository.findByUserIdAndDate(user.getId(), date).orElse(null);
    }

    /** ✅ Gemini 분석 결과 기반 오늘 식단 저장/갱신 */
    @Transactional
    public DailyMeal saveDailyMeal(User user, DailyAnalysis analysis) {
        LocalDate today = LocalDate.now();

        // 오늘 기록 조회 (없으면 새로 생성)
        DailyMeal meal = dailyMealRepository.findByUserIdAndDate(user.getId(), today)
                .orElse(DailyMeal.builder()
                        .user(user)
                        .date(today)
                        .totalCalories(0.0)
                        .totalProtein(0.0)
                        .totalFat(0.0)
                        .totalCarbs(0.0)
                        .build());

        // 기존 식단 JSON → List<MealEntry>
        List<MealEntry> meals = new ArrayList<>();
        if (meal.getMealsJson() != null && !meal.getMealsJson().isBlank()) {
            try {
                meals = objectMapper.readValue(meal.getMealsJson(), new TypeReference<>() {});
            } catch (Exception e) {
                System.err.println("⚠️ 기존 식단 JSON 파싱 실패: " + e.getMessage());
            }
        }

        // ---------------------
        // 기본 정리
        // ---------------------
        String action = analysis.getAction() == null ? "add" : analysis.getAction();
        String target = analysis.getTargetMeal();
        List<MealEntry> newMeals = analysis.getMeals() != null ? analysis.getMeals() : List.of();

        System.out.printf("📌 DailyMealService - action=%s, target=%s, newMeals=%d개%n",
                action, target, newMeals.size());

        // ---------------------------------------------------------
        // 🔧 update 보정 — 끼니가 1개면 targetMeal 강제 보정
        // ---------------------------------------------------------
        if ("update".equals(action) && target == null && newMeals.size() == 1) {
            target = newMeals.get(0).getTime();
            System.out.println("🔧 targetMeal 자동 보정 → " + target);
        }

        // ---------------------
        // 액션별 처리
        // ---------------------
        switch (action) {

            case "replace" -> {
                System.out.println("🔁 전체 식단 교체 (replace)");
                meals.clear();
                meals.addAll(newMeals);
            }

            case "update" -> {
                System.out.println("✏️ 식단 수정 감지 → " + target);

                if (!newMeals.isEmpty()) {

                    if (target != null) {
                        // 🎯 특정 끼니만 싹 지우고 새로 넣기
                        String finalTarget = target;
                        meals.removeIf(m -> finalTarget.equals(m.getTime()));
                    } else {
                        // 🧠 멀티 끼니 수정: 새로 들어온 끼니 time들 기준으로 기존 끼니 제거
                        var timesToReplace = newMeals.stream()
                                .map(MealEntry::getTime)
                                .collect(java.util.stream.Collectors.toSet());
                        meals.removeIf(m -> timesToReplace.contains(m.getTime()));
                        System.out.println("🔄 멀티 끼니 수정 → " + timesToReplace);
                    }

                    // 새 식단 추가
                    meals.addAll(newMeals);
                }
            }

            case "delete" -> {
                System.out.println("🗑️ 식단 삭제 감지 → " + target);

                if (target != null) {
                    String finalTarget = target;
                    meals.removeIf(m -> finalTarget.equals(m.getTime()));
                } else if (!newMeals.isEmpty()) {
                    var times = newMeals.stream()
                            .map(MealEntry::getTime)
                            .collect(java.util.stream.Collectors.toSet());
                    meals.removeIf(m -> times.contains(m.getTime()));
                }
            }

            default -> {
                System.out.println("➕ 식단 추가 감지 (add)");
                meals.addAll(newMeals);
            }
        }

        // ---------------------
        // 총합 재계산
        // ---------------------
        double totalKcal = 0, totalProtein = 0, totalFat = 0, totalCarbs = 0;

        for (MealEntry m : meals) {
            if (m.getFoods() == null) continue;
            for (FoodItem f : m.getFoods()) {
                if (f == null) continue;
                totalKcal   += f.getCalories() != null ? f.getCalories() : 0;
                totalProtein += f.getProtein()  != null ? f.getProtein()  : 0;
                totalFat    += f.getFat()      != null ? f.getFat()      : 0;
                totalCarbs  += f.getCarbs()    != null ? f.getCarbs()    : 0;
            }
        }

        // JSON 직렬화 후 저장
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

    /** ✅ 관리자가 직접 수정하는 경우 수동 저장용 */
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
