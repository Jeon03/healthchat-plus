package com.healthchat.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FoodItem {
    private String name;
    private double quantity;
    private String unit;

    // Edamam 계산 결과 추가
    private Double calories;
    private Double protein;
    private Double carbs;
    private Double fat;
}