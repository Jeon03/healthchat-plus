package com.healthchat.backend.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnifiedAnalysisResult {
    private DailyAnalysis mealAnalysis;
    private ExerciseAnalysisResult exerciseAnalysis;
    private EmotionSummaryDto emotionAnalysis;
}