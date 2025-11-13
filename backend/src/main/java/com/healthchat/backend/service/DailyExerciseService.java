package com.healthchat.backend.service;

import com.healthchat.backend.dto.ExerciseAnalysisResult;
import com.healthchat.backend.dto.ExerciseItemDto;
import com.healthchat.backend.entity.DailyActivity;
import com.healthchat.backend.entity.ExerciseItem;
import com.healthchat.backend.entity.User;
import com.healthchat.backend.repository.DailyActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;


@Service
@RequiredArgsConstructor
public class DailyExerciseService {

    private final DailyActivityRepository dailyActivityRepository;

    public DailyActivity getTodayActivity(User user) {
        return dailyActivityRepository.findByUserAndDate(user, LocalDate.now())
                .orElse(null);
    }

    public DailyActivity getActivityByDate(User user, LocalDate date) {
        return dailyActivityRepository.findByUserAndDate(user, date)
                .orElse(null);
    }
    /**
     * ✅ Gemini 분석 결과를 기반으로 하루 운동 데이터를 저장하거나 갱신
     */
    public DailyActivity saveOrUpdateDailyActivity(User user, ExerciseAnalysisResult analysis) {
        LocalDate today = LocalDate.now();

        DailyActivity activity = dailyActivityRepository.findByUserAndDate(user, today)
                .orElse(DailyActivity.builder()
                        .user(user)
                        .date(today)
                        .build());

        if (analysis == null || analysis.getAction() == null) {
            System.out.println("⚠️ 분석 결과가 비어 있음 — 저장하지 않음");
            return activity;
        }

        switch (analysis.getAction()) {
            case "add" -> addExercises(activity, analysis);
            case "update" -> updateExercises(activity, analysis);
            case "delete" -> deleteExercises(activity, analysis);
            case "replace" -> replaceExercises(activity, analysis);
            default -> System.out.println("⚠️ Unknown action: " + analysis.getAction());
        }

        activity.setTotalCalories(analysis.getTotalCalories());
        activity.setTotalDuration(analysis.getTotalDuration());

        return dailyActivityRepository.save(activity);
    }

    /**
     * 🟢 add — 새로운 운동 항목 추가
     */
    private void addExercises(DailyActivity activity, ExerciseAnalysisResult analysis) {
        if (analysis.getExercises() == null || analysis.getExercises().isEmpty()) return;

        analysis.getExercises().forEach(dto -> {
            ExerciseItem item = ExerciseItem.builder()
                    .category(dto.getCategory())
                    .part(dto.getPart())
                    .name(dto.getName())
                    .durationMin(dto.getDurationMin())
                    .intensity(dto.getIntensity())
                    .calories(dto.getCalories())
                    .activity(activity)
                    .build();
            activity.addExercise(item);
        });
    }

    /**
     * 🟡 update — 기존 항목을 수정 (단순히 replace로 처리)
     */
    private void updateExercises(DailyActivity activity, ExerciseAnalysisResult analysis) {
        replaceExercises(activity, analysis);
    }

    /**
     * 🔴 delete — 전달된 이름과 일치하는 운동 삭제
     */
    private void deleteExercises(DailyActivity activity, ExerciseAnalysisResult analysis) {
        if (analysis.getExercises() == null || analysis.getExercises().isEmpty()) return;

        List<String> names = analysis.getExercises().stream()
                .map(ExerciseItemDto::getName)
                .toList();

        activity.getExercises().removeIf(e -> names.contains(e.getName()));
    }

    /**
     * 🔵 replace — 기존 운동 전체를 새로 교체
     */
    private void replaceExercises(DailyActivity activity, ExerciseAnalysisResult analysis) {
        activity.getExercises().clear();
        addExercises(activity, analysis);
    }

    @Transactional
    public DailyActivity saveOrUpdateManual(User user, DailyActivity updated) {

        LocalDate date = updated.getDate() != null ? updated.getDate() : LocalDate.now();

        // 기존 데이터 조회 또는 새 객체 생성
        DailyActivity activity = dailyActivityRepository.findByUserAndDate(user, date)
                .orElse(DailyActivity.builder()
                        .user(user)
                        .date(date)
                        .build());

        // ⭐ 기존 운동 항목 전체 교체 (modify 기능)
        activity.getExercises().clear();

        if (updated.getExercises() != null) {
            for (ExerciseItem ex : updated.getExercises()) {
                ExerciseItem newItem = ExerciseItem.builder()
                        .activity(activity)
                        .name(ex.getName())
                        .durationMin(ex.getDurationMin())
                        .calories(ex.getCalories())
                        .category(ex.getCategory())
                        .part(ex.getPart())
                        .intensity(ex.getIntensity())
                        .build();
                activity.addExercise(newItem);
            }
        }

        // ⭐ 총합 다시 설정
        activity.setTotalCalories(updated.getTotalCalories());
        activity.setTotalDuration(updated.getTotalDuration());

        return dailyActivityRepository.save(activity);
    }
}
