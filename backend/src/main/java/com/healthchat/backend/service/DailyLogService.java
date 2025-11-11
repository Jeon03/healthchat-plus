package com.healthchat.backend.service;

import com.healthchat.backend.entity.DailyLog;
import com.healthchat.backend.entity.DailyMeal;
import com.healthchat.backend.entity.User;
import com.healthchat.backend.repository.DailyLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class DailyLogService {

    private final DailyLogRepository dailyLogRepository;

    public DailyLog updateDailyLog(User user, DailyMeal meal) {
        LocalDate today = LocalDate.now();

        // ✅ 오늘 기록 찾기
        DailyLog log = dailyLogRepository.findByUserIdAndDate(user.getId(), today)
                .orElse(DailyLog.builder()
                        .user(user)
                        .date(today)
                        .build());

        // ✅ 식단 업데이트
        log.setMeal(meal);
        log.setTotalCalories(meal.getTotalCalories());

        return dailyLogRepository.save(log);
    }
}
