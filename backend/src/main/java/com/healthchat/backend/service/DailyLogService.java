package com.healthchat.backend.service;

import com.healthchat.backend.entity.*;
import com.healthchat.backend.repository.DailyActivityRepository;
import com.healthchat.backend.repository.DailyEmotionRepository;
import com.healthchat.backend.repository.DailyLogRepository;
import com.healthchat.backend.repository.DailyMealRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class DailyLogService {

    private final DailyLogRepository dailyLogRepository;
    private final DailyActivityRepository dailyActivityRepository;
    private final DailyMealRepository dailyMealRepository;
    private final DailyEmotionRepository dailyEmotionRepository;


    /**
     * 공통 getOrCreate
     */
    public DailyLog getOrCreate(User user, LocalDate date) {
        return dailyLogRepository.findByUserIdAndDate(user.getId(), date)
                .orElseGet(() ->
                        dailyLogRepository.save(
                                DailyLog.builder()
                                        .user(user)
                                        .date(date)
                                        .build()
                        )
                );
    }


    // ========================================================================
    // 🥗 1) 식단 삭제 시 DailyLog 정리
    // ========================================================================
    @Transactional
    public void clearMeal(User user, LocalDate date) {
        dailyLogRepository.findByUserIdAndDate(user.getId(), date)
                .ifPresent(log -> {
                    log.setMeal(null);          // FK 제거
                    recalcSummary(log);         // 총칼로리 재계산
                    dailyLogRepository.save(log);
                });
    }


    // ========================================================================
    // 🏋 2) 운동 삭제 시 DailyLog 정리
    // ========================================================================
    @Transactional
    public void clearActivity(User user, LocalDate date) {
        dailyLogRepository.findByUserIdAndDate(user.getId(), date)
                .ifPresent(log -> {
                    log.setActivity(null);      // FK 제거
                    recalcSummary(log);
                    dailyLogRepository.save(log);
                });
    }


    // ========================================================================
    // 💬 3) 감정 삭제 시 DailyLog 정리
    // ========================================================================
    @Transactional
    public void clearEmotion(User user, LocalDate date) {
        dailyLogRepository.findByUserIdAndDate(user.getId(), date)
                .ifPresent(log -> {
                    log.setEmotion(null);
                    log.setMoodSummary(null);
                    dailyLogRepository.save(log);
                });
    }


    // ========================================================================
    // 🔥 4) 전체 로그 삭제 (오늘)
    // ========================================================================
    @Transactional
    public void deleteAll(User user) {
        deleteAll(user, LocalDate.now());
    }


    // ========================================================================
    // 🔥 5) 전체 로그 삭제 (특정 날짜)
    // ========================================================================
    @Transactional
    public void deleteAll(User user, LocalDate date) {

        // 0) FK 끊기 (DailyLog 내부 FK 제거)
        dailyLogRepository.findByUserIdAndDate(user.getId(), date)
                .ifPresent(log -> {
                    log.setMeal(null);
                    log.setActivity(null);
                    log.setEmotion(null);
                    log.setMoodSummary(null);
                    recalcSummary(log);
                    dailyLogRepository.save(log);
                });

        // 1) DailyLog 먼저 삭제
        dailyLogRepository.findByUserIdAndDate(user.getId(), date)
                .ifPresent(dailyLogRepository::delete);

        // 2) 부모(식단/운동/감정) 삭제
        dailyMealRepository.deleteByUserAndDate(user, date);
        dailyActivityRepository.deleteByUserAndDate(user, date);
        dailyEmotionRepository.deleteByUserAndDate(user, date);

        System.out.println("🗑 전체 기록 삭제 완료 (DailyLog + Meal + Activity + Emotion)");
    }


    // ========================================================================
    // 🔄 Summary 재계산
    // ========================================================================
    private void recalcSummary(DailyLog log) {

        double mealCalories = (log.getMeal() != null)
                ? log.getMeal().getTotalCalories()
                : 0;

        double exerciseCalories = (log.getActivity() != null)
                ? log.getActivity().getTotalCalories()
                : 0;

        double exerciseTime = (log.getActivity() != null)
                ? log.getActivity().getTotalDuration()
                : 0;

        log.setTotalCalories(mealCalories - exerciseCalories);
        log.setTotalExerciseTime(exerciseTime);
    }


    // ========================================================================
    // ⭐ 새로 추가하는 필수 메서드 3개
    // ========================================================================

    /** 하루 식단 업데이트 */
    @Transactional
    public DailyLog updateMeal(User user, DailyMeal meal) {
        LocalDate date = LocalDate.now();

        DailyLog log = getOrCreate(user, date);
        log.setMeal(meal);

        recalcSummary(log);
        return dailyLogRepository.save(log);
    }

    /** 하루 운동 업데이트 */
    @Transactional
    public DailyLog updateActivity(User user, DailyActivity activity) {
        LocalDate date = LocalDate.now();

        DailyLog log = getOrCreate(user, date);
        log.setActivity(activity);

        recalcSummary(log);
        return dailyLogRepository.save(log);
    }

    /** 하루 감정 업데이트 */
    @Transactional
    public DailyLog updateEmotion(User user, DailyEmotion emotion) {
        LocalDate date = LocalDate.now();

        DailyLog log = getOrCreate(user, date);
        log.setEmotion(emotion);

        if (emotion.getPrimaryEmotion() != null) {
            log.setMoodSummary(emotion.getPrimaryEmotion());
        }

        return dailyLogRepository.save(log);
    }
}
