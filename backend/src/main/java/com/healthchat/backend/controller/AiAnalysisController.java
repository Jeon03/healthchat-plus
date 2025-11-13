package com.healthchat.backend.controller;

import com.healthchat.backend.dto.DailyActivityResponseDto;
import com.healthchat.backend.dto.DailyAnalysis;
import com.healthchat.backend.dto.UnifiedAnalysisResult;
import com.healthchat.backend.entity.DailyActivity;
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
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai")
public class AiAnalysisController {

    private final DailyMealService dailyMealService;
    private final UserRepository userRepository;
    private final GeminiUnifiedAnalysisService geminiUnifiedAnalysisService;
    private final DailyExerciseService dailyExerciseService;
    private final RecommendedActivityService recommendedActivityService;

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

    /** ✅ 특정 날짜 운동 조회 */
    @GetMapping("/activity/{date}")
    public ResponseEntity<?> getActivityByDate(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable String date
    ) {
        if (user == null) return ResponseEntity.status(401).body("로그인 필요");

        LocalDate target = LocalDate.parse(date);

        User foundUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("사용자 없음"));

        DailyActivity activity = dailyExerciseService.getActivityByDate(foundUser, target);

        if (activity == null) {
            return ResponseEntity.ok("해당 날짜의 운동 데이터가 없습니다.");
        }

        return ResponseEntity.ok(activity);
    }

    @PostMapping("/analyze")
    public ResponseEntity<UnifiedAnalysisResult> analyzeAll(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody Map<String, String> req
    ) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        String text = req.getOrDefault("text", "");
        System.out.println("📥 통합 입력 텍스트: " + text);

        UnifiedAnalysisResult result = geminiUnifiedAnalysisService.analyzeAll(user.getId(), text);
        return ResponseEntity.ok(result);
    }
}
