package com.healthchat.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthchat.backend.config.GeminiClient;
import com.healthchat.backend.dto.DailyAnalysis;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 🧠 GeminiMealAnalysisService (v5)
 * - 외부 API 없이 Gemini만으로 식단 + 영양 분석 + 수정/추가/삭제 의도 인식
 */
@Service
@RequiredArgsConstructor
public class GeminiMealAnalysisService {

    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 🍱 사용자의 자연어 식단 입력 → DailyAnalysis 변환 (action 포함)
     */
    public DailyAnalysis analyzeDailyLog(String userText) {
        String prompt = buildPrompt(userText);
        String geminiResponse = geminiClient.generateJson(prompt);

        if (geminiResponse == null) {
            System.out.println("⚠️ Gemini 응답 없음 — fallback 사용");
            return buildFallbackAnalysis(userText);
        }

        String json = extractJson(geminiResponse);

        try {
            DailyAnalysis result = objectMapper.readValue(json, DailyAnalysis.class);

            // ✅ replace 오탐 교정 로직 추가
            if ("replace".equalsIgnoreCase(result.getAction())) {
                boolean mentionsSingleMeal = userText.matches(".*(아침|점심|저녁|간식).*");
                boolean mentionsFullReset = userText.matches(".*(오늘|식단|전체|다시|새로|처음부터|전부).*");

                if (mentionsSingleMeal && !mentionsFullReset) {
                    System.out.println("⚠️ replace 오탐 → update로 교정됨");
                    result.setAction("update");
                }
            }

            System.out.printf("✅ Gemini 분석 완료 → %s (%.0f kcal)%n",
                    result.getAction() == null ? "add" : result.getAction(),
                    result.getTotalCalories());
            return result;

        } catch (Exception e) {
            System.err.println("❌ Gemini JSON 파싱 실패: " + e.getMessage());
            System.err.println("⚠️ 응답 내용: " + geminiResponse);
            return buildFallbackAnalysis(userText);
        }
    }

    /**
     * 📋 Gemini 프롬프트 — 자연어 기반 식단 관리용
     */
    private String buildPrompt(String userText) {
        return """
    너는 사용자의 식단 기록을 관리하는 AI야.
    사용자의 문장을 분석해서 식단의 추가(add), 수정(update), 삭제(delete), 교체(replace) 중 어떤 의도인지 판단하고,
    끼니별로 음식 정보를 구조화된 JSON으로 반환해줘.

    📦 출력 JSON 예시:
    {
      "action": "add" | "update" | "delete" | "replace",
      "targetMeal": "아침" | "점심" | "저녁" | "간식" | null,
      "meals": [
        {
          "time": "아침" | "점심" | "저녁" | "간식",
          "foods": [
            {
              "name": "음식 이름(한국어)",
              "quantity": (숫자, g 단위),
              "unit": "g",
              "calories": (숫자, kcal),
              "protein": (숫자, g),
              "fat": (숫자, g),
              "carbs": (숫자, g)
            }
          ]
        }
      ],
      "totalCalories": (총 kcal),
      "totalProtein": (총 단백질 g),
      "totalFat": (총 지방 g),
      "totalCarbs": (총 탄수화물 g)
    }

    🧭 판단 규칙:
    - "말고", "대신", "수정", "바꿔" → action = "update"
    - "추가", "그리고", "또" → action = "add"
    - "빼", "삭제", "없애", "지워" → action = "delete"
    - "다시", "전체", "새로", "리셋", "처음부터", "전부", "다 갈아엎" → action = "replace"
    - 끼니(아침/점심/저녁/간식)를 인식해서 targetMeal에 지정
    - 영양 정보는 대략적으로 추정 (아래 기준 참고)

    ⚖️ 참고 영양 기준 (한국 음식):
    - 밥 1공기 ≈ 210g ≈ 300kcal (탄수화물 70g)
    - 라면 1봉지 ≈ 120g ≈ 500kcal (탄수화물 70g, 지방 16g)
    - 국수 1그릇 ≈ 300g ≈ 400kcal (탄수화물 60g)
    - 김치 ≈ 80g ≈ 30kcal
    - 계란 1개 ≈ 50g ≈ 70kcal (단백질 6g)
    - 우유 1컵 ≈ 200g ≈ 130kcal (단백질 6g, 지방 7g)

    ⚙️ 출력 규칙:
    - 반드시 JSON만 출력 (설명 금지)
    - meals 배열이 비어 있어도 action과 targetMeal은 포함해야 함

    입력:
    """ + userText;
    }

    /**
     * ✅ JSON만 추출
     */
    private String extractJson(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1).trim();
        }
        return text.trim();
    }

    /**
     * ✅ Gemini 실패 시 기본 구조 반환
     */
    private DailyAnalysis buildFallbackAnalysis(String userText) {
        System.out.println("⚠️ Gemini 분석 실패 — 재시도 요청 전송");

        return DailyAnalysis.builder()
                .action("error")   // ❗ 명확히 실패임을 표시
                .targetMeal(null)
                .meals(List.of())  // 비워두기
                .totalCalories(0.0)
                .totalProtein(0.0)
                .totalFat(0.0)
                .totalCarbs(0.0)
                .message("AI 분석 실패: 다시 시도해주세요.") // ⚠️ 새 필드 추가 (프론트용)
                .build();
    }
}
