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

    public DailyAnalysis analyzeDailyLog(String userText) {
        String prompt = buildPrompt(userText);
        String geminiResponse = geminiClient.generateJson("gemini-2.5-pro", prompt);

        if (geminiResponse == null) {
            System.out.println("⚠️ Gemini 응답 없음 — fallback 사용");
            return buildFallbackAnalysis(userText);
        }

        String json = extractJson(geminiResponse);

        try {
            DailyAnalysis result = objectMapper.readValue(json, DailyAnalysis.class);

            // ✅ replace 오탐 교정
            if ("replace".equalsIgnoreCase(result.getAction())) {
                boolean mentionsSingleMeal = userText.matches(".*(아침|점심|저녁|간식).*");
                boolean mentionsFullReset = userText.matches(".*(오늘|식단|전체|다시|새로|처음부터|전부).*");

                if (mentionsSingleMeal && !mentionsFullReset) {
                    System.out.println("⚠️ replace 오탐 → update로 교정됨");
                    result.setAction("update");
                }
            }

            // ✅ update인데 targetMeal이 없고 끼니가 1개뿐이면 자동 지정
            if ("update".equalsIgnoreCase(result.getAction())
                    && result.getTargetMeal() == null
                    && result.getMeals() != null
                    && result.getMeals().size() == 1) {

                String time = result.getMeals().get(0).getTime();
                result.setTargetMeal(time);
                System.out.println("🔧 update targetMeal 자동 설정 → " + time);
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

    private String buildPrompt(String userText) {
        return """
너는 사용자의 식단 기록을 관리하는 전문 AI 어시스턴트야.
입력 문장을 기반으로 사용자가 원하는 '의도(action)'를 정확하게 판단하고,
끼니 단위로 음식 정보를 정교한 JSON 형식으로 출력해야 한다.

📌 너의 가장 중요한 역할:
1) 사용자가 의도한 변경 범위(추가/수정/삭제/전체교체)를 정확히 구분할 것
2) 끼니별로 데이터를 구조적으로 반환할 것
3) 단일 끼니 수정인지, 여러 끼니 수정인지 명확히 판단할 것
4) targetMeal 누락 시 자동 보정 규칙을 적용할 것

--------------------------------------------
🧠 [Action 판단 규칙 — 매우 엄격히 적용]
--------------------------------------------
• add (추가)
  - "그리고", "또", "추가", "같이 먹었어", "더" 포함
  - 기존 기록을 유지하면서 새 음식만 붙임

• update (부분 수정)
  - "수정", "바꿔", "변경", "말고", "대신" 포함
  - 특정 끼니를 새 내용으로 대체
  - 문장에 여러 끼니가 있으면 "다중 끼니 수정"으로 처리

• delete (삭제)
  - "빼", "제거", "삭제", "없애", "지워" 포함
  - targetMeal 또는 제거할 음식 단위로 처리

• replace (전체 교체)
  - "다시", "처음부터", "전체", "전부", "새로", "올 갈아엎어", "식단 다시 알려줄게"
  - 오늘 날짜의 기록을 전부 초기화한 후 새 식단만 저장

--------------------------------------------
🎯 [targetMeal 보정 규칙]
--------------------------------------------
- 문장 내에 "아침/점심/저녁/간식"이 명확히 등장하면 그 끼니를 targetMeal로 설정
- 여러 끼니가 등장하면 targetMeal = null (여러 끼니 동시 처리)
- 아침/점심/저녁 중 단 하나의 끼니만 새로 입력되면 targetMeal = 그 끼니
- 문장에서 끼니가 언급되지 않아도
  → 사용자가 기존 식단을 바꾸는 표현("말고", "대신")이 있다면 update로 해석하고 targetMeal = null

--------------------------------------------
📦 [반드시 출력할 JSON 스키마]
--------------------------------------------
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

--------------------------------------------
⚠️ [출력 규칙]
--------------------------------------------
- 반드시 JSON만 출력 (설명 금지)
- meals가 비어 있어도 action과 targetMeal은 반드시 포함
- 음식의 단위는 반드시 "g"
- 감정/운동/추천 등의 문장은 절대 넣지 말 것
- JSON 바깥에 다른 글자 출력 금지

--------------------------------------------
📥 입력:
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
