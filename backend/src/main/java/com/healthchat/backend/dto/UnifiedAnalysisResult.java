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
    private ExerciseAnalysisResult exerciseAnalysis; // ✅ 단일 객체로 변경
//    private EmotionAnalysisResult emotionAnalysis;   // (추후 감정 분석용)
}