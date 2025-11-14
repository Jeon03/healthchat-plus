package com.healthchat.backend.service;

import com.healthchat.backend.dto.*;
import com.healthchat.backend.entity.DailyActivity;
import com.healthchat.backend.entity.DailyEmotion;
import com.healthchat.backend.entity.DailyMeal;
import com.healthchat.backend.entity.User;
import com.healthchat.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class GeminiUnifiedAnalysisService {

    private final GeminiRoutingService routingService;

    private final GeminiMealAnalysisService mealService;
    private final GeminiExerciseAnalysisService exerciseService;
    private final GeminiEmotionAnalysisService emotionService;

    private final UserRepository userRepository;
    private final DailyMealService dailyMealService;
    private final DailyExerciseService dailyExerciseService;
    private final DailyEmotionService dailyEmotionService;
    private final DailyLogService dailyLogService;

    public UnifiedAnalysisResult analyzeAll(Long userId, String userText) {

        // 1️⃣ 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자 없음"));

        // 2️⃣ 문장 라우팅
        var routed = routingService.route(userText);

        // 3️⃣ 비동기로 각각 실행 (전부 @Async 메서드)
        CompletableFuture<DailyAnalysis> mealFuture =
                routed.mealText().isBlank()
                        ? CompletableFuture.completedFuture(null)
                        : mealService.analyzeDailyLog(routed.mealText());

        CompletableFuture<ExerciseAnalysisResult> exerciseFuture =
                routed.exerciseText().isBlank()
                        ? CompletableFuture.completedFuture(null)
                        : exerciseService.analyzeExercise(userId, routed.exerciseText());

        CompletableFuture<EmotionAnalysisResult> emotionFuture =
                routed.emotionText().isBlank()
                        ? CompletableFuture.completedFuture(null)
                        : emotionService.analyzeEmotion(routed.emotionText());

        // 4️⃣ 모두 완료될 때까지 기다림
        CompletableFuture.allOf(mealFuture, exerciseFuture, emotionFuture).join();

        // 5️⃣ 결과 추출
        DailyAnalysis mealAnalysis = mealFuture.join();
        ExerciseAnalysisResult exerciseAnalysis = exerciseFuture.join();
        EmotionAnalysisResult emotionAnalysis = emotionFuture.join();

        // 6️⃣ DB 저장
        DailyMeal savedMeal = null;
        if (mealAnalysis != null) {
            savedMeal = dailyMealService.saveDailyMeal(user, mealAnalysis);
        }

        DailyActivity savedActivity = null;
        if (exerciseAnalysis != null) {
            savedActivity = dailyExerciseService.saveOrUpdateDailyActivity(user, exerciseAnalysis);
        }

        DailyEmotion savedEmotion = null;
        EmotionSummaryDto emotionSummaryDto = null;
        if (emotionAnalysis != null) {
            savedEmotion = dailyEmotionService.saveDailyEmotion(user, emotionAnalysis);
            emotionSummaryDto = dailyEmotionService.toSummaryDto(savedEmotion);
        }

        // 7️⃣ 하루 종합 로그 업데이트
        dailyLogService.updateDailyLog(user, savedMeal, savedActivity);

        // 8️⃣ 최종 통합 응답
        return UnifiedAnalysisResult.builder()
                .mealAnalysis(mealAnalysis)
                .exerciseAnalysis(exerciseAnalysis)
                .emotionAnalysis(emotionSummaryDto)
                .build();
    }
}

