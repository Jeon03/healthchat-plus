package com.healthchat.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthchat.backend.config.GeminiClient;
import com.healthchat.backend.dto.ExerciseAnalysisResult;
import com.healthchat.backend.entity.User;
import com.healthchat.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class GeminiExerciseAnalysisService {

    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UserRepository userRepository;

    long start = System.currentTimeMillis();
    /**
     * 💪 사용자의 자연어 운동 입력 → Gemini JSON 파싱
     */
    @Async
    public CompletableFuture<ExerciseAnalysisResult> analyzeExercise(Long userId, String userText) {

        // 1) 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 2) 프롬프트 생성
        String prompt = buildPrompt(user, userText);

        // 3) Gemini 호출
        String response = geminiClient.generateJson("gemini-2.5-pro", prompt);

        if (response == null || response.isBlank()) {
            System.out.println("⚠️ Gemini 응답이 비어 있음 — fallback 사용");
            return CompletableFuture.completedFuture(fallback());
        }

        // 4) JSON 추출
        String json = extractJson(response);

        try {
            // 5) JSON → DTO 파싱
            ExerciseAnalysisResult result =
                    objectMapper.readValue(json, ExerciseAnalysisResult.class);

            long took = System.currentTimeMillis() - start;

            int duration = 0;
            try {
                Object raw = result.getTotalDuration();
                if (raw != null) {
                    duration = (int) Math.round(Double.parseDouble(raw.toString()));
                }
            } catch (Exception ignore) {}

            System.out.printf(
                    "✅ [Exercise] Gemini 운동 분석 완료: %s (%.0f kcal, %d분) — %dms%n",
                    result.getAction(),
                    result.getTotalCalories(),
                    duration,
                    took
            );

            return CompletableFuture.completedFuture(result);

        } catch (Exception e) {
            System.err.println("❌ Gemini JSON 파싱 실패: " + e.getMessage());
            System.err.println("⚠️ 원문 응답: " + response);

            return CompletableFuture.completedFuture(fallback());
        }
    }



    private String buildPrompt(User user, String userText) {

        String gender = safe(user.getGender());
        double height = safeDouble(user.getHeight());
        double weight = safeDouble(user.getWeight());
        double bmi = (height > 0 && weight > 0)
                ? weight / Math.pow(height / 100.0, 2)
                : 0.0;
        int age = calculateAge(user.getBirthDate());
        String goal = safe(user.getGoalText());

        String profileInfo =
                "- 성별: " + gender + "\n" +
                        "- 나이: " + age + "세\n" +
                        "- 키: " + height + "cm\n" +
                        "- 몸무게: " + weight + "kg\n" +
                        "- BMI: " + String.format("%.1f", bmi) + "\n" +
                        "- 목표: " + goal + "\n";

        return """
        너는 사용자의 운동 기록을 구조화하여 JSON으로 변환하는 운동 분석 AI야.
        문장에는 식단/감정 내용이 섞여 있을 수 있으므로 **운동 관련 내용만** 분석해야 한다.

        ------------------------------------------------------
        🔥 목적
        - 사용자가 한 운동을 정확하게 추출하고
        - action(add/update/delete/replace)을 정확히 판단하며
        - 총 운동 시간/칼로리를 계산한다.

        ------------------------------------------------------
        🧍 사용자 정보(이 정보를 반드시 칼로리·강도 계산에 반영해야 한다):
        """
                + profileInfo +
                """
                ------------------------------------------------------
                📌 [운동 관련 문장 인식 규칙]
                다음 단어가 포함되면 운동으로 판단:
                - 걷기, 뛰기, 달리기, 조깅, 러닝
                - 자전거, 사이클, 수영
                - 푸시업, 스쿼트, 데드리프트, 벤치프레스, 플랭크 등
                - ~~분 운동, ~~시간 운동
                - 헬스, 웨이트, 근력운동, 유산소, 스트레칭
        
                🚫 다음 단어가 포함된 문장은 무시해야 한다(식단/감정):
                - 먹다, 마시다, 밥, 라면, 샐러드 등
                - 기분, 우울, 행복, 스트레스 등 감정 관련 단어
        
                ------------------------------------------------------
                🧠 [action 판단 규칙]
        
                🔄 replace (전체 다시 작성)
                - “전체”, “전부”, “다시”, “리셋”, “처음부터”, “전체 수정”
        
                ✏ update (일부 수정)
                - “수정”, “바꿔”, “대신”, “말고”
        
                ➕ add (추가)
                - “그리고”, “또”, “추가로”, “더”
        
                🗑 delete (삭제)
                - “지워”, “없애”, “삭제”, “빼줘”
        
                ------------------------------------------------------
                📦 [출력 JSON 형식 — 반드시 이 형태만 출력해야 한다]
        
                {
                  "action": "add" | "update" | "delete" | "replace",
                  "exercises": [
                    {
                      "category": "CARDIO" | "STRENGTH" | "PILATES" | "YOGA" | "STRETCHING" | "OTHER",
                      "part": "FULL" | "CHEST" | "BACK" | "LOWER" | "ABS" | "SHOULDER" | "ARM" | "OTHER",
                      "name": "운동 이름",
                      "durationMin": 숫자(분),
                      "intensity": "LOW" | "MEDIUM" | "HIGH",
                      "calories": 숫자(kcal)
                    }
                  ],
                  "totalCalories": 숫자,
                  "totalDuration": 숫자
                }
        
                ------------------------------------------------------
                📏 [칼로리 산정 규칙]
                - 유산소(CARDIO)는 체중×시간을 기반으로 현실적 계산
                - 근력운동(STRENGTH)은 강도/intensity 적용
                - 강도는 LOW/MEDIUM/HIGH 중 하나 선택
                - 운동 시간이 명확하지 않으면 기본 15분 가정
        
                ------------------------------------------------------
                📝 분석할 문장:
                """ + userText;
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
