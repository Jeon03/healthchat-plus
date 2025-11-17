package com.healthchat.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthchat.backend.config.GeminiClient;
import com.healthchat.backend.dto.AiCoachFeedbackDto;
import com.healthchat.backend.entity.DailyEmotion;
import com.healthchat.backend.entity.DailyLog;
import com.healthchat.backend.entity.User;
import com.healthchat.backend.repository.DailyLogRepository;
import com.healthchat.backend.repository.UserRepository;
import com.healthchat.backend.service.rag.GuidelineSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiCoachService {

    private final UserRepository userRepository;
    private final DailyLogRepository dailyLogRepository;
    private final DailyEmotionService dailyEmotionService;
    private final GuidelineSearchService guidelineSearchService;
    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;

    /**
     * ==========================================
     *  AI 건강 코치 메인 로직
     * ==========================================
     */
    public AiCoachFeedbackDto generateDailyFeedback(Long userId, LocalDate date) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자 없음"));

        DailyLog dailyLog = dailyLogRepository.findByUserIdAndDate(userId, date)
                .orElseThrow(() -> new RuntimeException("해당 날짜의 DailyLog 없음"));

        DailyEmotion emotion = dailyEmotionService.getEmotionByDate(user, date);

        String analysisQuery = buildAnalysisQuery(user, dailyLog, emotion);

        List<GuidelineSearchService.RetrievedChunk> chunks =
                guidelineSearchService.searchRelevantChunks(analysisQuery);

        String prompt = buildGeminiPrompt(user, dailyLog, emotion, chunks);

        // 🔥 Gemini 호출
        String response = geminiClient.generateSmartJson(prompt);

        if (response == null || response.isBlank()) {
            log.error("⚠️ Gemini 응답 null/공백 → fallback 실행");
            return fallbackFeedback(user, dailyLog, emotion);
        }

        String json = extractJson(response);

        if (json == null || json.isBlank() || !json.trim().startsWith("{")) {
            log.error("⚠️ 추출된 JSON 형식 오류: {}", json);
            return fallbackFeedback(user, dailyLog, emotion);
        }

        try {
            return objectMapper.readValue(json, AiCoachFeedbackDto.class);
        } catch (Exception e) {
            log.error("❌ JSON 파싱 오류: {}", e.getMessage());
            log.error("원문 JSON: {}", json);
            return fallbackFeedback(user, dailyLog, emotion);
        }
    }


    /**
     * ⭐ 여러 목표 및 요인 전체 출력
     */
    private String buildGoalsSection(User user) {
        List<User.GoalDetail> goals = user.getParsedGoals();

        if (goals.isEmpty()) {
            return "===== [사용자의 목표] =====\n등록된 목표 없음\n\n";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("===== [사용자의 목표] =====\n");

        int idx = 1;
        for (User.GoalDetail g : goals) {
            sb.append(idx++).append(". 목표: ").append(g.getGoal()).append("\n");

            if (g.getFactors() != null && !g.getFactors().isEmpty()) {
                sb.append("   - 주요 요인:\n");
                for (String f : g.getFactors()) {
                    sb.append("     • ").append(f).append("\n");
                }
            }
            sb.append("\n");
        }

        return sb.toString();
    }


    /**
     * ==========================================================
     * Gemini 응답의 ```json 코드블록 정리 + JSON만 추출
     * ==========================================================
     */
    private String extractJson(String text) {
        if (text == null || text.isBlank()) return null;

        text = text.replace("```json", "")
                .replace("```", "")
                .trim();

        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');

        if (start < 0 || end <= start) {
            return null;
        }

        return text.substring(start, end + 1).trim();
    }


    /**
     * ==========================================================
     * buildAnalysisQuery — 간단 요약 (Gemini 검색용)
     * ==========================================================
     */
    private String buildAnalysisQuery(User user, DailyLog log, DailyEmotion emotion) {
        StringBuilder sb = new StringBuilder();

        sb.append("사용자의 하루 건강 상태를 요약해줘.\n\n");

        sb.append("■ 사용자 프로필\n");
        sb.append("- 나이: ").append(user.getBirthDate()).append("\n");
        sb.append("- 성별: ").append(user.getGender()).append("\n");
        sb.append("- 키: ").append(user.getHeight()).append("\n");
        sb.append("- 몸무게: ").append(user.getWeight()).append("\n");

        // ⭐ 기존 goalText → 여러 목표 출력으로 변경
        sb.append("- 목표:\n");
        for (User.GoalDetail g : user.getParsedGoals()) {
            sb.append("   • ").append(g.getGoal()).append("\n");
        }
        sb.append("\n");

        // ⭐ 디버그 출력도 goalsDetail 기반으로 변경
        System.out.println("========== 사용자 프로필 ==========");
        System.out.println("성별       : " + user.getGender());
        System.out.println("생년월일   : " + user.getBirthDate());
        System.out.println("키         : " + user.getHeight());
        System.out.println("몸무게     : " + user.getWeight());
        System.out.println("목표       : ");
        for (User.GoalDetail g : user.getParsedGoals()) {
            System.out.println("  - " + g.getGoal());
        }
        System.out.println("===================================");

        sb.append("■ 오늘 요약\n");

        if (log.getMeal() != null) {
            sb.append("- 총 섭취 칼로리: ").append(log.getMeal().getTotalCalories()).append("\n");
        } else {
            sb.append("- 식단 기록 없음\n");
        }

        if (log.getActivity() != null) {
            sb.append("- 운동 소모 칼로리: ").append(log.getActivity().getTotalCalories()).append("\n");
        } else {
            sb.append("- 운동 기록 없음\n");
        }

        sb.append("- 순 에너지: ").append(log.getTotalCalories()).append("\n");

        if (emotion != null) {
            sb.append("■ 감정 요약\n");
            sb.append("- 대표 감정: ").append(emotion.getPrimaryEmotion()).append("\n");
            sb.append("- 감정 요약: ").append(emotion.getSummariesJson()).append("\n");
        } else {
            sb.append("■ 감정 기록 없음\n");
        }

        return sb.toString();
    }



    /**
     * ==========================================================
     * Gemini 프롬프트 생성 — 여기 목표 섹션 포함됨 ⭐
     * ==========================================================
     */
    private String buildGeminiPrompt(
            User user,
            DailyLog dailyLog,
            DailyEmotion emotion,
            List<GuidelineSearchService.RetrievedChunk> chunks
    ) {

        StringBuilder sb = new StringBuilder();

        sb.append("너는 '개인 맞춤형 AI 건강 코치'야.\n")
                .append("사용자의 목표, 식단, 운동, 감정, 그리고 문헌 근거를 기반으로 코칭해야 해.\n\n");

        // === 문헌 근거 ===
        sb.append("===== [문헌 근거] =====\n");
        if (chunks != null && !chunks.isEmpty()) {
            for (var c : chunks) {
                sb.append("출처: ").append(c.source).append("\n");
                sb.append(c.text).append("\n\n");
            }
        } else {
            sb.append("(관련 문헌 없음)\n\n");
        }

        // === 사용자 정보 ===
        sb.append("===== [사용자 정보] =====\n");
        sb.append("- 성별: ").append(user.getGender()).append("\n");
        sb.append("- 생년월일: ").append(user.getBirthDate()).append("\n");
        sb.append("- 키: ").append(user.getHeight()).append("\n");
        sb.append("- 몸무게: ").append(user.getWeight()).append("\n");
        sb.append("- 목표 체중: ").append(user.getGoalWeight()).append("\n");
        sb.append("- 평균 수면: ").append(user.getAvgSleep()).append("\n");
        sb.append("- 알레르기: ").append(user.getAllergiesText()).append("\n");
        sb.append("- 복용약: ").append(user.getMedicationsText()).append("\n\n");

        // ⭐ 여러 목표 + 요인 전부 포함
        sb.append(buildGoalsSection(user)).append("\n");

        // === 오늘 운동 null-safe ===
        double exerciseCalories = dailyLog.getActivity() != null ? dailyLog.getActivity().getTotalCalories() : 0;
        double exerciseTime = dailyLog.getActivity() != null ? dailyLog.getActivity().getTotalDuration() : 0;

        // === 오늘 식단 null-safe ===
        double mealCalories = dailyLog.getMeal() != null ? dailyLog.getMeal().getTotalCalories() : 0;

        sb.append("===== [오늘 기록 요약] =====\n");
        sb.append("- 섭취 칼로리: ").append(mealCalories).append("\n");
        sb.append("- 운동 칼로리: ").append(exerciseCalories).append("\n");
        sb.append("- 운동 시간: ").append(exerciseTime).append("\n");
        sb.append("- 순 에너지: ").append(dailyLog.getTotalCalories()).append("\n");

        if (emotion != null) {
            sb.append("- 감정: ").append(emotion.getPrimaryEmotion()).append("\n");
            sb.append("- 감정 원인: ").append(emotion.getSummariesJson()).append("\n");
        }
        sb.append("\n");

        // === JSON 출력 형식 ===
        sb.append("===== [출력 형식(JSON)] =====\n")
                .append("설명 없이 아래 JSON만 출력해.\n\n")
                .append("```json\n")
                .append("{\n")
                .append("  \"summary\": \"하루를 간단히 요약\",\n")
                .append("  \"dietAdvice\": \"식단 조언\",\n")
                .append("  \"exerciseAdvice\": \"운동 조언\",\n")
                .append("  \"emotionAdvice\": \"감정 조언\",\n")
                .append("  \"goalAlignment\": \"오늘 기록이 목표와 얼마나 맞는지\",\n")
                .append("  \"references\": [\n")
                .append("    {\n")
                .append("      \"source\": \"문헌 출처\",\n")
                .append("      \"snippet\": \"관련 문헌 발췌\",\n")
                .append("      \"comment\": \"이 문헌이 왜 이 조언을 뒷받침하는지\"\n")
                .append("    }\n")
                .append("  ]\n")
                .append("}\n")
                .append("```\n");

        return sb.toString();
    }


    /**
     * ==========================================================
     * Gemini 실패 시 fallback
     * ==========================================================
     */
    private AiCoachFeedbackDto fallbackFeedback(User user, DailyLog log, DailyEmotion emotion) {
        return AiCoachFeedbackDto.builder()
                .summary("AI 분석 오류가 발생했습니다. 기본 피드백을 제공합니다.")
                .dietAdvice("가급적 단백질/식이섬유 중심 식단으로 균형을 유지해보세요.")
                .exerciseAdvice("매일 20~30분이라도 가벼운 운동을 시도해보세요.")
                .emotionAdvice("감정이 불안정한 날에는 충분한 휴식을 챙겨보세요.")
                .goalAlignment("AI 분석이 가능해지면 목표 적합도를 더 세밀하게 제공해드릴게요.")
                .references(List.of())
                .build();
    }
}
