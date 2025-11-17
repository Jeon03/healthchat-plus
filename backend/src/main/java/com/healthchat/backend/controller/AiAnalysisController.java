package com.healthchat.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthchat.backend.dto.DailyActivityResponseDto;
import com.healthchat.backend.dto.DailyAnalysis;
import com.healthchat.backend.dto.UnifiedAnalysisResult;
import com.healthchat.backend.entity.DailyActivity;
import com.healthchat.backend.entity.DailyEmotion;
import com.healthchat.backend.entity.DailyMeal;
import com.healthchat.backend.entity.User;
import com.healthchat.backend.repository.UserRepository;
import com.healthchat.backend.security.CustomUserDetails;
import com.healthchat.backend.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai")
public class AiAnalysisController {

    private final DailyMealService dailyMealService;
    private final UserRepository userRepository;
    private final GeminiUnifiedAnalysisService geminiUnifiedAnalysisService;
    private final DailyEmotionService dailyEmotionService;
    private final DailyExerciseService dailyExerciseService;
    private final RecommendedActivityService recommendedActivityService;
    private final GeminiRoutingService routingService;
    private final ObjectMapper mapper = new ObjectMapper();
    private final DailyLogService dailyLogService;
    private final AiCoachFeedbackService aiCoachFeedbackService;

    private <T> T fromJson(String json, Class<T> type) {
        try {
            return mapper.readValue(json, type);
        } catch (Exception e) {
            return null;
        }
    }
    @PostMapping("/meals/save")
    public ResponseEntity<DailyMeal> saveManual(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody DailyMeal updatedMeal
    ) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        User foundUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("사용자 없음"));

        DailyMeal saved = dailyMealService.saveOrUpdateManual(foundUser, updatedMeal);
        return ResponseEntity.ok(saved);
    }

    /** ✅ 오늘의 식단 조회 */
    @GetMapping("/meals/today")
    public ResponseEntity<?> getTodayMeals(@AuthenticationPrincipal CustomUserDetails user) {
        if (user == null) return ResponseEntity.status(401).body("로그인 필요");

        User foundUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("사용자 없음"));

        DailyMeal todayMeal = dailyMealService.getTodayMeal(foundUser);

        if (todayMeal == null) {
            return ResponseEntity.ok("오늘 등록된 식단이 없습니다.");
        }
        return ResponseEntity.ok(todayMeal);
    }

    /** ✅ 특정 날짜 식단 조회 */
    @GetMapping("/meals/{date}")
    public ResponseEntity<?> getMealsByDate(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable String date
    ) {
        if (user == null) return ResponseEntity.status(401).body("로그인 필요");

        LocalDate target = LocalDate.parse(date);
        User foundUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("사용자 없음"));

        DailyMeal targetMeal = dailyMealService.getMealByDate(foundUser, target);
        if (targetMeal == null) {
            return ResponseEntity.ok("해당 날짜의 식단이 없습니다.");
        }
        return ResponseEntity.ok(targetMeal);
    }



    @PostMapping("/activity/save")
    public ResponseEntity<DailyActivity> saveActivity(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody DailyActivity updated
    ) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        User foundUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("사용자 없음"));

        DailyActivity saved = dailyExerciseService.saveOrUpdateManual(foundUser, updated);
        return ResponseEntity.ok(saved);
    }

    /** ✅ 오늘의 운동 조회 */
    @GetMapping("/activity/today")
    public ResponseEntity<?> getTodayActivity(
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        if (user == null) return ResponseEntity.status(401).body("로그인 필요");

        User foundUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("사용자 없음"));

        DailyActivity today = dailyExerciseService.getTodayActivity(foundUser);

        double recommended = recommendedActivityService.calculateRecommendedBurn(foundUser);

        if (today == null) {
            return ResponseEntity.ok(
                    DailyActivityResponseDto.builder()
                            .activity(null)
                            .recommendedBurn(recommended)
                            .build()
            );
        }

        return ResponseEntity.ok(
                DailyActivityResponseDto.builder()
                        .activity(today)
                        .recommendedBurn(recommended)
                        .build()
        );
    }

    /** 특정 날짜 운동 조회 */
    @GetMapping("/activity/{date}")
    public ResponseEntity<?> getActivityByDate(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable String date
    ) {
        if (user == null) {
            Map<String, Object> body = new HashMap<>();
            body.put("activity", null);
            body.put("recommendedBurn", 0);
            return ResponseEntity.status(401).body(body);
        }

        LocalDate target = LocalDate.parse(date);

        User foundUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("사용자 없음"));

        DailyActivity activity = dailyExerciseService.getActivityByDate(foundUser, target);

        double recommended = recommendedActivityService.calculateRecommendedBurn(foundUser);

        Map<String, Object> result = new HashMap<>();
        result.put("activity", activity);            // null 허용
        result.put("recommendedBurn", recommended); // double → boxing but fine

        return ResponseEntity.ok(result);
    }



    /** 오늘의 감정 조회 */
    @GetMapping("/emotion/today")
    public ResponseEntity<?> getTodayEmotion(@AuthenticationPrincipal CustomUserDetails user) {
        if (user == null) return ResponseEntity.status(401).body("로그인 필요");

        User foundUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("사용자 없음"));

        DailyEmotion todayEmotion = dailyEmotionService.getTodayEmotion(foundUser);

        if (todayEmotion == null) {
            return ResponseEntity.ok("오늘 감정 기록이 없습니다.");
        }

        // ⭐ DTO 변환 후 반환
        return ResponseEntity.ok(dailyEmotionService.toSummaryDto(todayEmotion));
    }


    /** 특정 날짜 감정 조회 */
    @GetMapping("/emotion/{date}")
    public ResponseEntity<?> getEmotionByDate(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable String date
    ) {
        if (user == null) return ResponseEntity.status(401).body("로그인 필요");

        LocalDate target = LocalDate.parse(date);

        User foundUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("사용자 없음"));

        DailyEmotion emotion = dailyEmotionService.getEmotionByDate(foundUser, target);

        if (emotion == null) {
            return ResponseEntity.ok("해당 날짜의 감정 데이터가 없습니다.");
        }

        // ⭐ DTO 변환 후 반환
        return ResponseEntity.ok(dailyEmotionService.toSummaryDto(emotion));
    }


    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeAll(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody Map<String, String> req
    ) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        // 🔥 User 단 1번 조회
        User foundUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("사용자 없음"));

        String text = req.getOrDefault("text", "");
        System.out.println("📥 통합 입력 텍스트: " + text);

        // 🔥 라우팅도 딱 1번!
        var routed = routingService.route(text);

        boolean isDeleteAll =
                "전체 기록 삭제".equals(routed.mealText()) &&
                        "전체 기록 삭제".equals(routed.exerciseText()) &&
                        "전체 기록 삭제".equals(routed.emotionText());

        // 🔥 전체 삭제 처리
        if (isDeleteAll) {

            dailyLogService.deleteAll(foundUser, LocalDate.now());
            dailyMealService.deleteToday(foundUser);
            dailyExerciseService.deleteToday(foundUser);
            dailyEmotionService.deleteToday(foundUser);
            aiCoachFeedbackService.deleteTodayFeedback(foundUser.getId());

            return ResponseEntity.ok(Map.of(
                    "message", "오늘의 전체 기록을 삭제했습니다.",
                    "deleted", true
            ));
        }

        // 🔥 라우팅 결과 + User 직접 전달하도록 변경
        UnifiedAnalysisResult result =
                geminiUnifiedAnalysisService.analyzeAll(
                        foundUser,
                        text,
                        routed
                );

        return ResponseEntity.ok(result);
    }


}
