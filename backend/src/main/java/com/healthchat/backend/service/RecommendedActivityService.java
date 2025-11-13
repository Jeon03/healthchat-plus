package com.healthchat.backend.service;

import com.healthchat.backend.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;

@Service
@RequiredArgsConstructor
public class RecommendedActivityService {

    public double calculateRecommendedBurn(User user) {

        double weight = user.getWeight();
        double height = user.getHeight();
        int age = calculateAge(user.getBirthDate());

        // 성별 계산
        double bmr;
        if (user.getGender() != null && user.getGender().equalsIgnoreCase("male")) {
            bmr = 10 * weight + 6.25 * height - 5 * age + 5;
        } else {
            bmr = 10 * weight + 6.25 * height - 5 * age - 161;
        }

        // 활동량 계수
        double tdee = bmr * 1.375;

        // 목표 기반 운동 칼로리
        String goal = user.getGoalText() == null ? "" : user.getGoalText();

        if (goal.contains("감량") || goal.contains("다이어트")) {
            return Math.round(tdee * 0.22); // 약 22% 소모 목표
        } else if (goal.contains("증가")) {
            return Math.round(tdee * 0.15);
        } else {
            return Math.round(tdee * 0.18); // 유지
        }
    }

    private int calculateAge(LocalDate birthDate) {
        if (birthDate == null) return 30; // 기본값
        return Period.between(birthDate, LocalDate.now()).getYears();
    }
}
