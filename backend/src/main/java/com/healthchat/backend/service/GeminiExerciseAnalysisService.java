package com.healthchat.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthchat.backend.config.GeminiClient;
import com.healthchat.backend.dto.ExerciseAnalysisResult;
import com.healthchat.backend.entity.User;
import com.healthchat.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GeminiExerciseAnalysisService {

    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UserRepository userRepository;

    /**
     * 💪 사용자의 자연어 운동 입력 → Gemini JSON 파싱
     */
    public ExerciseAnalysisResult analyzeExercise(Long userId, String userText) {
        // ✅ 1. 사용자 정보 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // ✅ 2. 프롬프트 생성
        String prompt = buildPrompt(user, userText);

        // ✅ 3. Gemini 호출
        String response = geminiClient.generateJson(prompt);

        if (response == null || response.isBlank()) {
            System.out.println("⚠️ Gemini 응답이 비어 있음 — fallback 사용");
            return fallback();
        }

        // ✅ 4. JSON 부분만 추출
        String json = extractJson(response);

        try {
            // ✅ 5. JSON 파싱 (단일 객체)
            ExerciseAnalysisResult result = objectMapper.readValue(json, ExerciseAnalysisResult.class);
            System.out.printf("✅ Gemini 운동 분석 완료: %s (%.0f kcal)\n",
                    result.getAction(), result.getTotalCalories());
            return result;
        } catch (Exception e) {
            System.err.println("❌ Gemini JSON 파싱 실패: " + e.getMessage());
            System.err.println("⚠️ 원문 응답: " + response);
            return fallback();
        }
    }


    /**
     * 📋 Gemini 프롬프트 — 한국어 기반 운동 분석
     */
    private String buildPrompt(User user, String userText) {
        String gender = safe(user.getGender());
        double height = safeDouble(user.getHeight());
        double weight = safeDouble(user.getWeight());
        double bmi = (height > 0 && weight > 0)
                ? weight / Math.pow(height / 100.0, 2)
                : 0.0;
        int age = calculateAge(user.getBirthDate());
        String goal = safe(user.getGoalText());

        return """
        너는 개인 맞춤형 피트니스 코치야.
        사용자가 입력한 문장을 분석해서 어떤 운동을 했는지, 어떤 의도(action)인지, 총 운동 시간과 칼로리를 추정해줘.

        👤 사용자 프로필:
        - 성별: %s
        - 나이: %d세
        - 키: %.1fcm
        - 몸무게: %.1fkg
        - BMI: %.1f
        - 목표: %s

        🎯 작업 지침:
        1️⃣ 사용자의 문장에서 의도를 파악해 아래 중 하나를 지정해줘.
            - "했어", "추가", "새로" → action = "add"
            - "수정", "바꿔" → action = "update"
            - "삭제", "없애", "지워" → action = "delete"
            - "다시", "전체", "새로 시작" → action = "replace"

        2️⃣ 사용자가 언급한 모든 운동을 추출해서 아래 형식의 JSON으로 정리해줘.
            각 운동별로 운동 종류, 부위, 이름, 시간, 강도, 칼로리를 포함해야 해.

        📦 출력 JSON 예시:
        {
          "action": "add",
          "exercises": [
            {
              "category": "STRENGTH" | "CARDIO" | "YOGA" | "PILATES" | "STRETCHING" | "OTHER",
              "part": "CHEST" | "SHOULDER" | "BACK" | "ABS" | "LOWER" | "FULL" | "OTHER",
              "name": "운동 이름",
              "durationMin": (숫자, 분 단위),
              "intensity": "LOW" | "MEDIUM" | "HIGH",
              "calories": (숫자, kcal)
            }
          ],
          "totalCalories": (숫자, 총 칼로리 kcal),
          "totalDuration": (숫자, 총 운동시간 분)
        }

        ⚙️ 규칙:
        - 반드시 JSON만 출력 (설명, 해설 금지)
        - 칼로리 계산 시 나이, 성별, 체중, BMI, 목표를 참고해서 현실적인 값을 추정
        - 운동이 여러 개일 경우 각각 나열
        - 알 수 없는 경우 "기타"로 처리

        입력 문장:
        "%s"
        """.formatted(gender, age, height, weight, bmi, goal, userText);
    }

    /**
     * 🧮 생년월일로 나이 계산
     */
    private int calculateAge(LocalDate birthDate) {
        if (birthDate == null) return 0;
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    /**
     * ✅ JSON만 추출
     */
    private String extractJson(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start)
            return text.substring(start, end + 1).trim();
        return text.trim();
    }

    private String safe(String s) {
        return (s == null || s.isBlank()) ? "Unknown" : s;
    }

    private double safeDouble(Double d) {
        return (d == null) ? 0.0 : d;
    }

    private ExerciseAnalysisResult fallback() {
        return ExerciseAnalysisResult.builder()
                .action("error")
                .exercises(List.of())
                .totalCalories(0)
                .totalDuration(0)
                .message("AI 분석 실패: 다시 시도해주세요.")
                .build();
    }
}
