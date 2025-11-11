package com.healthchat.backend.controller;

import com.healthchat.backend.dto.DailyAnalysis;
import com.healthchat.backend.entity.DailyMeal;
import com.healthchat.backend.entity.User;
import com.healthchat.backend.repository.UserRepository;
import com.healthchat.backend.security.CustomUserDetails;
import com.healthchat.backend.service.DailyLogService;
import com.healthchat.backend.service.DailyMealService;
import com.healthchat.backend.service.GeminiAnalysisService;
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

    private final GeminiAnalysisService geminiAnalysisService;
    private final DailyMealService dailyMealService;
    private final UserRepository userRepository;
    private final DailyLogService dailyLogService;


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



    @PostMapping("/meals")
    public ResponseEntity<DailyAnalysis> analyzeAndSave(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody Map<String, String> req
    ) {
        if (user == null) {
            throw new RuntimeException("로그인 필요");
        }

        String text = req.getOrDefault("text", "");
        System.out.println("📥 입력 텍스트: " + text);

        // 1️⃣ Gemini 분석 (식단 + 영양 포함)
        DailyAnalysis analysis = geminiAnalysisService.analyzeDailyLog(text);

        // 2️⃣ DB 저장
        User foundUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("사용자 없음"));

        // ✅ 식단 저장
        var meal = dailyMealService.saveDailyMeal(foundUser, analysis);

        // ✅ 하루 통합 로그에도 반영
        dailyLogService.updateDailyLog(foundUser, meal);

        // 3️⃣ 결과 반환
        return ResponseEntity.ok(analysis);
    }
}
