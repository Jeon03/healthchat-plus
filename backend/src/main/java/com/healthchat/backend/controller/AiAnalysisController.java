package com.healthchat.backend.controller;

import com.healthchat.backend.dto.DailyAnalysis;
import com.healthchat.backend.entity.User;
import com.healthchat.backend.repository.UserRepository;
import com.healthchat.backend.security.CustomUserDetails;
import com.healthchat.backend.service.DailyMealService;
import com.healthchat.backend.service.DailyNutritionService;
import com.healthchat.backend.service.GeminiAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai")
public class AiAnalysisController {

    private final GeminiAnalysisService geminiAnalysisService;
    private final DailyNutritionService dailyNutritionService;
    private final DailyMealService dailyMealService;
    private final UserRepository userRepository;

    /**
     * ✅ 하루 식단 분석 + Edamam 영양계산 + DB 저장
     */
    @PostMapping("/meals")
    public ResponseEntity<DailyAnalysis> analyzeAndSave(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody Map<String, String> req
    ) {
        if (user == null) {
            throw new RuntimeException("❌ 로그인 필요");
        }

        String text = req.getOrDefault("text", "");
        System.out.println("📥 입력 텍스트: " + text);

        // 1️⃣ Gemini 분석
        DailyAnalysis analysis = geminiAnalysisService.analyzeDailyLog(text);

        // 2️⃣ Edamam 영양정보 계산
        analysis = dailyNutritionService.processMeals(analysis);

        // 3️⃣ DB 저장
        User foundUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("❌ 사용자 없음"));
        dailyMealService.saveDailyMeal(foundUser, analysis);

        // 4️⃣ 결과 반환
        return ResponseEntity.ok(analysis);
    }
}
