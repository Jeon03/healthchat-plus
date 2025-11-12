package com.healthchat.backend.service;

import com.healthchat.backend.dto.*;
import com.healthchat.backend.entity.User;
import com.healthchat.backend.entity.DailyMeal;
import com.healthchat.backend.entity.DailyActivity;
import com.healthchat.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 🧠 GeminiUnifiedAnalysisService
 * - 식단 + 운동 (추후 감정 포함 예정)
 * - Gemini 분석 결과를 통합 처리 및 DB 반영
 */
@Service
@RequiredArgsConstructor
public class GeminiUnifiedAnalysisService {

    private final GeminiMealAnalysisService mealService;
    private final GeminiExerciseAnalysisService exerciseService;
//    private final GeminiEmotionAnalysisService emotionService;

    private final UserRepository userRepository;
    private final DailyMealService dailyMealService;
    private final DailyExerciseService dailyExerciseService;
    private final DailyLogService dailyLogService;

    public UnifiedAnalysisResult analyzeAll(Long userId, String userText) {
        // ✅ 1. 사용자 정보 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자 없음"));

        // ✅ 2. 식단 분석
        DailyAnalysis mealAnalysis = mealService.analyzeDailyLog(userText);

        // ✅ 3. 운동 분석
        ExerciseAnalysisResult exerciseAnalysis = exerciseService.analyzeExercise(userId, userText);

        // ✅ 4. DB 저장
        DailyMeal meal = dailyMealService.saveDailyMeal(user, mealAnalysis);
        DailyActivity activity = dailyExerciseService.saveOrUpdateDailyActivity(user, exerciseAnalysis);

        // ✅ 5. 하루 통합 로그 업데이트 (식단 + 운동)
        dailyLogService.updateDailyLog(user, meal, activity);

        // ✅ 6. 통합 결과 반환
        return UnifiedAnalysisResult.builder()
                .mealAnalysis(mealAnalysis)
                .exerciseAnalysis(exerciseAnalysis)
//                .emotionAnalysis(emotionAnalysis)
                .build();
    }
}
