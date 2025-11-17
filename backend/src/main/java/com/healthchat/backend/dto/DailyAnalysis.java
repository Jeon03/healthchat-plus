package com.healthchat.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyAnalysis {
    private String action;        // add | update | delete
    private String targetMeal;    // 아침 | 점심 | 저녁 | 간식
    private List<MealEntry> meals;
    private String message;
    private double totalCalories;
    private double totalProtein;
    private double totalFat;
    private double totalCarbs;

    /** 🔥 삭제 응답 */
    public static DailyAnalysis deleted(String type) {
        return DailyAnalysis.builder()
                .action("delete")
                .targetMeal(null)
                .meals(List.of())
                .totalCalories(0.0)
                .totalProtein(0.0)
                .totalFat(0.0)
                .totalCarbs(0.0)
                .message(type + " 기록이 삭제되었습니다.")
                .build();
    }

    /** 🔥 비어있는 경우 (식단 입력 없음) */
    public static DailyAnalysis empty(String type) {
        return DailyAnalysis.builder()
                .action("none")
                .targetMeal(null)
                .meals(List.of())
                .totalCalories(0.0)
                .totalProtein(0.0)
                .totalFat(0.0)
                .totalCarbs(0.0)
                .message(type + " 입력이 감지되지 않았습니다.")
                .build();
    }
}

