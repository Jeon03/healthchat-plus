package com.healthchat.backend.service;

import com.healthchat.backend.dto.*;
import com.healthchat.backend.entity.User;
import com.healthchat.backend.entity.DailyMeal;
import com.healthchat.backend.entity.DailyActivity;
import com.healthchat.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GeminiUnifiedAnalysisService {

    private final GeminiRoutingService routingService; // ⭐ NEW!
    private final GeminiMealAnalysisService mealService;
    private final GeminiExerciseAnalysisService exerciseService;
    // private final GeminiEmotionAnalysisService emotionService;

    private final UserRepository userRepository;
    private final DailyMealService dailyMealService;
    private final DailyExerciseService dailyExerciseService;
    private final DailyLogService dailyLogService;

    public UnifiedAnalysisResult analyzeAll(Long userId, String userText) {

        // 1️⃣ 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자 없음"));

        // 2️⃣ 문장 라우팅 (식단/운동/감정 분리)
        var routed = routingService.route(userText);

        // 3️⃣ 각각 분석 실행
        DailyAnalysis mealAnalysis = null;
        if (!routed.mealText().isBlank()) {
            mealAnalysis = mealService.analyzeDailyLog(routed.mealText());
        }

        ExerciseAnalysisResult exerciseAnalysis = null;
        if (!routed.exerciseText().isBlank()) {
            exerciseAnalysis = exerciseService.analyzeExercise(userId, routed.exerciseText());
        }

        // Emotion 분석기는 나중에 붙일 수 있음
        // EmotionResult emotionAnalysis = null;
        // if (!routed.emotionText().isBlank()) {
        //     emotionAnalysis = emotionService.analyzeEmotion(userId, routed.emotionText());
        // }

        // 4️⃣ DB 업데이트 (null이면 업데이트 안함)
        DailyMeal meal = null;
        if (mealAnalysis != null) {
            meal = dailyMealService.saveDailyMeal(user, mealAnalysis);
        }

        DailyActivity activity = null;
        if (exerciseAnalysis != null) {
            activity = dailyExerciseService.saveOrUpdateDailyActivity(user, exerciseAnalysis);
        }

        // 5️⃣ 하루 종합 로그 업데이트
        dailyLogService.updateDailyLog(user, meal, activity);

        // 6️⃣ 통합 결과 반환
        return UnifiedAnalysisResult.builder()
                .mealAnalysis(mealAnalysis)
                .exerciseAnalysis(exerciseAnalysis)
                // .emotionAnalysis(emotionAnalysis)
                .build();
    }
}
