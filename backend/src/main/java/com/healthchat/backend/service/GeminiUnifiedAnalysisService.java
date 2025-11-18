package com.healthchat.backend.service;

import com.healthchat.backend.dto.*;
import com.healthchat.backend.entity.*;
import com.healthchat.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class GeminiUnifiedAnalysisService {

    private final GeminiMealAnalysisService mealService;
    private final GeminiExerciseAnalysisService exerciseService;
    private final GeminiEmotionAnalysisService emotionService;
    private final AiCoachFeedbackService aiCoachFeedbackService;
    private final UserRepository userRepository;
    private final DailyMealService dailyMealService;
    private final DailyExerciseService dailyExerciseService;
    private final DailyEmotionService dailyEmotionService;
    private final DailyLogService dailyLogService;

    /**
     * 컨트롤러에서 라우팅 결과를 전달받는 버전
     */
    public UnifiedAnalysisResult analyzeAll(
            User user, String text, GeminiRoutingService.RoutingResult routed
    ) {

        /* =====================================================================
           🔥 1) 먼저 삭제 명령을 처리한다 (AI 호출 안 함)
           ===================================================================== */

        // -------------------------
        // 전체 삭제
        // -------------------------
        if ("DELETE_ALL".equals(routed.mealText())
                && "DELETE_ALL".equals(routed.exerciseText())
                && "DELETE_ALL".equals(routed.emotionText())) {

            dailyMealService.deleteToday(user);
            dailyExerciseService.deleteToday(user);
            dailyEmotionService.deleteToday(user);
            dailyLogService.deleteAll(user);
            aiCoachFeedbackService.deleteTodayFeedback(user.getId());
            return UnifiedAnalysisResult.builder()
                    .mealAnalysis(DailyAnalysis.deleted("meal"))
                    .exerciseAnalysis(ExerciseAnalysisResult.deleted())
                    .emotionAnalysis(EmotionSummaryDto.deleted())
                    .build();
        }

        /* -------------------------
           개별 삭제 (식단)
           ------------------------- */
        CompletableFuture<DailyAnalysis> mealFuture;
        if ("DELETE_MEAL".equals(routed.mealText())) {

            dailyMealService.deleteToday(user);

            mealFuture = CompletableFuture.completedFuture(
                    DailyAnalysis.deleted("meal")
            );

        } else {
            // AI 호출
            mealFuture = routed.mealText().isBlank()
                    ? CompletableFuture.completedFuture(DailyAnalysis.empty("meal"))
                    : mealService.analyzeDailyLog(user, routed.mealText());
        }


        /* -------------------------
           개별 삭제 (운동)
           ------------------------- */
        CompletableFuture<ExerciseAnalysisResult> exerciseFuture;

        if ("DELETE_EXERCISE".equals(routed.exerciseText())) {

            dailyExerciseService.deleteToday(user);

            exerciseFuture = CompletableFuture.completedFuture(
                    ExerciseAnalysisResult.deleted()
            );

        } else {

            String exerciseText = routed.exerciseText();
            List<ExerciseItem> todayExercises = dailyExerciseService.getTodayExercises(user);

            if (exerciseText.isBlank()) {
                exerciseFuture = CompletableFuture.completedFuture(null);
            } else {
                exerciseFuture = exerciseService.analyzeExercise(
                        user.getId(),
                        exerciseText,
                        todayExercises
                );
            }
        }

/* -------------------------
   개별 삭제 (감정)
   ------------------------- */
        CompletableFuture<EmotionAnalysisResult> emotionFuture;

        String emoText = routed.emotionText();

        if ("DELETE_EMOTION".equalsIgnoreCase(emoText)) {

            dailyEmotionService.deleteToday(user);

            emotionFuture = CompletableFuture.completedFuture(
                    EmotionAnalysisResult.deleted()
            );

        } else if (emoText == null || emoText.isBlank()) {

            emotionFuture = CompletableFuture.completedFuture(null);

        } else {

            emotionFuture = emotionService.analyzeEmotion(emoText);
        }


        /* =====================================================================
           🔥 2) AI Future들 모두 기다림
           ===================================================================== */
        CompletableFuture.allOf(mealFuture, exerciseFuture, emotionFuture).join();

        DailyAnalysis mealAnalysis = mealFuture.join();
        ExerciseAnalysisResult exerciseAnalysis = exerciseFuture.join();
        EmotionAnalysisResult emotionAnalysis = emotionFuture.join();


        /* =====================================================================
           🔥 3) DB 반영
           ===================================================================== */

        DailyMeal savedMeal = null;
        if (mealAnalysis != null && !"delete".equals(mealAnalysis.getAction())) {
            savedMeal = dailyMealService.saveDailyMeal(user, mealAnalysis);
            dailyLogService.updateMeal(user, savedMeal);
        }

        DailyActivity savedActivity = null;
        if (exerciseAnalysis != null && !"delete".equals(exerciseAnalysis.getAction())) {
            savedActivity = dailyExerciseService.saveOrUpdateDailyActivity(user, exerciseAnalysis);
            dailyLogService.updateActivity(user, savedActivity);
        }

        EmotionSummaryDto responseEmotionDto = null;

        if (emotionAnalysis != null) {

            // ✅ 1) 삭제 액션이면: DB 저장 X, 삭제용 DTO를 그대로 응답
            if ("delete".equals(emotionAnalysis.getAction())) {
                responseEmotionDto = EmotionSummaryDto.deleted();
            }
            // ✅ 2) 정상 분석이면: 저장 후 Summary DTO로 변환
            else {
                DailyEmotion savedEmotion = dailyEmotionService.saveDailyEmotion(user, emotionAnalysis);
                dailyLogService.updateEmotion(user, savedEmotion);
                responseEmotionDto = dailyEmotionService.toSummaryDto(savedEmotion);
            }
        }


        /* =====================================================================
           🔥 4) 최종 응답
           ===================================================================== */
        return UnifiedAnalysisResult.builder()
                .mealAnalysis(mealAnalysis)
                .exerciseAnalysis(exerciseAnalysis)
                .emotionAnalysis(responseEmotionDto)
                .build();
    }
}

