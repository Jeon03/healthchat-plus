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
    private List<MealEntry> meals;      // 아침/점심/저녁
    private Double totalCalories;       // 총 칼로리
    private Double totalProtein;        // 총 단백질
    private Double totalFat;            // 총 지방
    private Double totalCarbs;          // 총 탄수화물
}