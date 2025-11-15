package com.healthchat.backend.service;

import com.healthchat.backend.entity.*;
import com.healthchat.backend.repository.DailyLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class DailyLogService {

    private final DailyLogRepository dailyLogRepository;

    /**
     * ✅ (공통) 날짜 기반 로그 조회 또는 생성
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

    /**
     * ✅ 하루 식단 업데이트
     */
    public DailyLog updateMeal(User user, DailyMeal meal) {
        LocalDate today = LocalDate.now();

        DailyLog log = getOrCreate(user, today);
        log.setMeal(meal);

        recalcSummary(log);

        return dailyLogRepository.save(log);
    }

    /**
     * ✅ 하루 운동 업데이트
     */
    public DailyLog updateActivity(User user, DailyActivity activity) {
        LocalDate today = LocalDate.now();

        DailyLog log = getOrCreate(user, today);
        log.setActivity(activity);

        recalcSummary(log);

        return dailyLogRepository.save(log);
    }

    /**
     * ✅ 하루 감정(DailyEmotion) 업데이트
     */
    public DailyLog updateEmotion(User user, DailyEmotion emotion) {
        LocalDate today = LocalDate.now();

        DailyLog log = getOrCreate(user, today);
        log.setEmotion(emotion);

        // 감정 요약 자동 계산 가능 (primaryEmotion 기반)
        if (emotion.getPrimaryEmotion() != null) {
            log.setMoodSummary(emotion.getPrimaryEmotion());
        }

        return dailyLogRepository.save(log);
    }

    /**
     * ✅ (중요) 하루 통합 요약 계산
     */
    private void recalcSummary(DailyLog log) {
        double mealCalories = (log.getMeal() != null) ? log.getMeal().getTotalCalories() : 0;
        double exerciseCalories = (log.getActivity() != null) ? log.getActivity().getTotalCalories() : 0;
        double exerciseTime = (log.getActivity() != null) ? log.getActivity().getTotalDuration() : 0;

        log.setTotalCalories(mealCalories - exerciseCalories);
        log.setTotalExerciseTime(exerciseTime);
    }
}
