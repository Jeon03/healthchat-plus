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
}