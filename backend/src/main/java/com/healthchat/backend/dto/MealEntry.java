package com.healthchat.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MealEntry {
    private String time;            // breakfast | lunch | dinner | snack
    private List<FoodItem> foods;   // 여러 음식 포함
}