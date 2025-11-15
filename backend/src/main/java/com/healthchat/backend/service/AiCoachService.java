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

        // 1) 유저 / 로그 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자 없음"));

        DailyLog dailyLog = dailyLogRepository.findByUserIdAndDate(userId, date)
                .orElseThrow(() -> new RuntimeException("해당 날짜의 DailyLog 없음"));

        DailyEmotion emotion = dailyEmotionService.getEmotionByDate(user, date);

        // 2) 자연어 기반 요약 쿼리 생성
        String analysisQuery = buildAnalysisQuery(user, dailyLog, emotion);

        // 3) RAG 기반 문헌 청크 검색
        List<GuidelineSearchService.RetrievedChunk> chunks =
                guidelineSearchService.searchRelevantChunks(analysisQuery);

        // 4) Gemini에 전달할 프롬프트 생성
        String prompt = buildGeminiPrompt(user, dailyLog, emotion, chunks);

        // 5) Gemini 호출
        String json = geminiClient.generateJson("gemini-2.5-pro", prompt);

        try {
            // ⭐ JSON 블록 제거 및 순수 JSON만 추출
            json = extractJson(json);

            // JSON → DTO 변환
            AiCoachFeedbackDto dto = objectMapper.readValue(json, AiCoachFeedbackDto.class);
            log.info("✅ AiCoach 피드백 생성 완료: user={}, date={}", userId, date);
            return dto;

        } catch (Exception e) {
            log.error("❌ AiCoach JSON 파싱 실패: {}", e.getMessage());
            log.error("원문 응답: {}", json);
            return fallbackFeedback(user, dailyLog, emotion);
        }
    }


    /**
     * ==========================================================
     * Gemini 응답의 ```json 코드블록 정리 + JSON만 추출
     * ==========================================================
     */
    private String extractJson(String text) {
        if (text == null) return null;

        // ```json, ``` 제거
        text = text.replace("```json", "")
                .replace("```", "")
                .trim();

        // JSON 범위만 추출
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');

        if (start >= 0 && end > start) {
            return text.substring(start, end + 1).trim();
        }

        return text; // 혹시 몰라 fallback
    }


    /**
     * ==========================================================
     * 유저 + 하루 데이터를 기반으로 Gemini에 넘길 요약 쿼리 생성
     * ==========================================================
     */
    private String buildAnalysisQuery(User user, DailyLog log, DailyEmotion emotion) {
        StringBuilder sb = new StringBuilder();

        sb.append("사용자의 하루 건강 상태를 요약하고, 식단·운동·감정 관점에서 분석해줘.\n\n");

        sb.append("■ 사용자 프로필\n");
        sb.append("- 나이: ").append(user.getBirthDate()).append("\n");
        sb.append("- 성별: ").append(user.getGender()).append("\n");
        sb.append("- 키: ").append(user.getHeight()).append("\n");
        sb.append("- 몸무게: ").append(user.getWeight()).append("\n");

        sb.append("- 목표: ").append(user.getGoalText()).append("\n\n");

        sb.append("■ 오늘 요약\n");
        sb.append("- 총 섭취 칼로리: ").append(log.getMeal() != null ? log.getMeal().getTotalCalories() : 0).append("\n");
        sb.append("- 운동 소모 칼로리: ").append(log.getActivity() != null ? log.getActivity().getTotalCalories() : 0).append("\n");
        sb.append("- 순 에너지: ").append(log.getTotalCalories()).append("\n");

        if (emotion != null) {
            sb.append("■ 감정 요약\n");
            sb.append("- 대표 감정: ").append(emotion.getPrimaryEmotion()).append("\n");
            sb.append("- 감정 요약: ").append(emotion.getSummariesJson()).append("\n");
        }

        return sb.toString();
    }



    /**
     * ==========================================================
     * Gemini 프롬프트 생성
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

        sb.append("===== [문헌 근거] =====\n");
        if (chunks != null && !chunks.isEmpty()) {
            for (var c : chunks) {
                sb.append("출처: ").append(c.source).append("\n");
                sb.append(c.text).append("\n\n");
            }
        } else {
            sb.append("(관련 문헌 없음)\n\n");
        }

        sb.append("===== [사용자 정보] =====\n");
        sb.append("- 성별: ").append(user.getGender()).append("\n");
        sb.append("- 생년월일: ").append(user.getBirthDate()).append("\n");
        sb.append("- 키: ").append(user.getHeight()).append("\n");
        sb.append("- 몸무게: ").append(user.getWeight()).append("\n");
        sb.append("- 목표 체중: ").append(user.getGoalWeight()).append("\n");
        sb.append("- 평균 수면: ").append(user.getAvgSleep()).append("\n");
        sb.append("- 알레르기: ").append(user.getAllergiesText()).append("\n");
        sb.append("- 복용약: ").append(user.getMedicationsText()).append("\n\n");

        sb.append("===== [사용자 목표] =====\n");
        if (user.getParsedGoals() != null && !user.getParsedGoals().isEmpty()) {
            for (var g : user.getParsedGoals()) {
                sb.append("- 목표: ").append(g.getGoal()).append("\n");
                sb.append("  이유: ").append(String.join(", ", g.getFactors())).append("\n");
            }
        } else {
            sb.append("(목표 정보 없음)\n");
        }
        sb.append("\n");

        sb.append("===== [오늘 기록 요약] =====\n");
        sb.append("- 섭취 칼로리: ").append(dailyLog.getMeal() != null ? dailyLog.getMeal().getTotalCalories() : 0).append("\n");
        sb.append("- 운동 칼로리: ").append(dailyLog.getActivity() != null ? dailyLog.getActivity().getTotalCalories() : 0).append("\n");
        sb.append("- 운동 시간: ").append(dailyLog.getTotalExerciseTime()).append("\n");
        sb.append("- 순 에너지: ").append(dailyLog.getTotalCalories()).append("\n");

        if (emotion != null) {
            sb.append("- 감정: ").append(emotion.getPrimaryEmotion()).append("\n");
            sb.append("- 감정 원인: ").append(emotion.getSummariesJson()).append("\n");
        }
        sb.append("\n");

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
     * Gemini 실패 시 제공되는 fallback 기본 피드백
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
