package com.healthchat.backend.service;

import com.healthchat.backend.dto.ExerciseAnalysisResult;
import com.healthchat.backend.dto.ExerciseItemDto;
import com.healthchat.backend.entity.DailyActivity;
import com.healthchat.backend.entity.ExerciseItem;
import com.healthchat.backend.entity.User;
import com.healthchat.backend.repository.DailyActivityRepository;
import com.healthchat.backend.repository.DailyEmotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DailyExerciseService {

    private final DailyEmotionRepository emotionRepository;
    private final DailyActivityRepository dailyActivityRepository;
    private final DailyLogService dailyLogService;
    public DailyActivity getTodayActivity(User user) {
        return dailyActivityRepository.findByUserAndDate(user, LocalDate.now())
                .orElse(null);
    }

    public DailyActivity getActivityByDate(User user, LocalDate date) {
        return dailyActivityRepository.findByUserAndDate(user, date)
                .orElse(null);
    }

    @Transactional
    public void deleteToday(User user) {

        LocalDate today = LocalDate.now();

        // 1️⃣ 감정 테이블 삭제
        emotionRepository.deleteByUserAndDate(user, today);

        // 2️⃣ DailyLog에서도 감정 정보 제거
        dailyLogService.clearEmotion(user, today);

        System.out.println("🗑 감정 기록 전체 삭제 완료");
    }

    @Transactional
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

            case "add" -> addOrMerge(activity, analysis);

            case "update" -> updateExercises(activity, analysis);

            case "delete" -> {
                boolean deleteRow = deleteExercises(activity, analysis);

                if (deleteRow) {
                    // 💥 먼저 daily_log.activity_id 를 NULL 처리
                    dailyLogService.clearActivity(user, LocalDate.now());

                    // 💥 그 다음 Activity row 삭제
                    dailyActivityRepository.delete(activity);

                    return null;
                }
            }

            case "replace" -> replaceExercises(activity, analysis);

            default -> System.out.println("⚠ Unknown action: " + analysis.getAction());
        }

        updateTotals(activity);
        return dailyActivityRepository.save(activity);
    }

    /**
     * 🔵 총합 재계산
     */
    private void updateTotals(DailyActivity activity) {
        double totalCalories = activity.getExercises()
                .stream()
                .mapToDouble(ExerciseItem::getCalories)
                .sum();

        double totalDuration = activity.getExercises()
                .stream()
                .mapToDouble(ExerciseItem::getDurationMin)
                .sum();

        activity.setTotalCalories(totalCalories);
        activity.setTotalDuration(totalDuration);
    }


    /**
     * 🟢 add + merge (중복이면 합산)
     */
    private void addOrMerge(DailyActivity activity, ExerciseAnalysisResult analysis) {
        if (analysis.getExercises() == null || analysis.getExercises().isEmpty()) return;

        analysis.getExercises().forEach(dto -> {

            ExerciseItem existing = activity.getExercises().stream()
                    .filter(e -> e.getName().equalsIgnoreCase(dto.getName()))
                    .findFirst().orElse(null);

            if (existing != null) {
                // 🔥 중복 운동 → 시간 + 칼로리 합산
                existing.setDurationMin(existing.getDurationMin() + dto.getDurationMin());
                existing.setCalories(existing.getCalories() + dto.getCalories());
                return;
            }

            // 🔥 신규 운동 → add
            ExerciseItem item = ExerciseItem.builder()
                    .activity(activity)
                    .name(dto.getName())
                    .category(dto.getCategory())
                    .part(dto.getPart())
                    .durationMin(dto.getDurationMin())
                    .intensity(dto.getIntensity())
                    .calories(dto.getCalories())
                    .build();

            activity.addExercise(item);
        });
    }


    /**
     * 🟡 update — 기존 운동을 "부분 수정"
     * (없는 운동을 수정하려 할 경우 무시)
     */
    private void updateExercises(DailyActivity activity, ExerciseAnalysisResult analysis) {
        if (analysis.getExercises() == null || analysis.getExercises().isEmpty()) return;

        analysis.getExercises().forEach(dto -> {

            ExerciseItem existing = activity.getExercises().stream()
                    .filter(e -> e.getName().equalsIgnoreCase(dto.getName()))
                    .findFirst().orElse(null);

            if (existing == null) return; // 수정할 대상이 없으면 skip

            existing.setDurationMin(dto.getDurationMin());
            existing.setCalories(dto.getCalories());
            existing.setCategory(dto.getCategory());
            existing.setPart(dto.getPart());
            existing.setIntensity(dto.getIntensity());
        });
    }


    /**
     * 🟥 delete — 전체 삭제 시 true 반환
     */
    private boolean deleteExercises(DailyActivity activity, ExerciseAnalysisResult analysis) {

        // ❗ 아무 운동 이름도 안 들어오면 → 전체 삭제
        if (analysis.getExercises() == null || analysis.getExercises().isEmpty()) {
            return true;
        }

        // 특정 운동만 삭제
        List<String> names = analysis.getExercises().stream()
                .map(ExerciseItemDto::getName)
                .toList();

        activity.getExercises().removeIf(e -> names.contains(e.getName()));

        return false;
    }


    /**
     * 🔵 replace — 전체 교체
     */
    private void replaceExercises(DailyActivity activity, ExerciseAnalysisResult analysis) {
        activity.getExercises().clear();
        addOrMerge(activity, analysis);
    }


    /**
     * ✍ 수동 수정
     */
    @Transactional
    public DailyActivity saveOrUpdateManual(User user, DailyActivity updated) {

        LocalDate date = updated.getDate() != null ? updated.getDate() : LocalDate.now();

        DailyActivity activity = dailyActivityRepository.findByUserAndDate(user, date)
                .orElse(DailyActivity.builder()
                        .user(user)
                        .date(date)
                        .build());

        activity.getExercises().clear();

        if (updated.getExercises() != null) {
            updated.getExercises().forEach(ex -> {
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
            });
        }

        updateTotals(activity);
        return dailyActivityRepository.save(activity);
    }

    public List<ExerciseItem> getTodayExercises(User user) {
        DailyActivity activity = dailyActivityRepository
                .findByUserAndDate(user, LocalDate.now())
                .orElse(null);

        if (activity == null) return List.of();
        return activity.getExercises();
    }
}
