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
     * ✅ 하루 통합 로그 업데이트 (식단 + 운동)
     */
    public DailyLog updateDailyLog(User user, DailyMeal meal, DailyActivity activity) {
        LocalDate today = LocalDate.now();

        // ✅ 오늘 로그 찾기 (없으면 새로 생성)
        DailyLog log = dailyLogRepository.findByUserIdAndDate(user.getId(), today)
                .orElse(DailyLog.builder()
                        .user(user)
                        .date(today)
                        .build());

        // ✅ 식단 및 운동 데이터 업데이트
        log.setMeal(meal);
        log.setActivity(activity);

        double mealCalories = (meal != null) ? meal.getTotalCalories() : 0;
        double exerciseCalories = (activity != null) ? activity.getTotalCalories() : 0;
        double exerciseTime = (activity != null) ? activity.getTotalDuration() : 0;

        // ✅ 하루 요약 통계 계산
        log.setTotalCalories(mealCalories - exerciseCalories);
        log.setTotalExerciseTime(exerciseTime);

        return dailyLogRepository.save(log);
    }
}
