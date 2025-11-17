package com.healthchat.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthchat.backend.config.GeminiClient;
import com.healthchat.backend.dto.DailyAnalysis;
import com.healthchat.backend.dto.MealEntry;
import com.healthchat.backend.entity.DailyMeal;
import com.healthchat.backend.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GeminiMealAnalysisService {

    private final GeminiClient geminiClient;
    private final DailyMealService dailyMealService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Async
    public CompletableFuture<DailyAnalysis> analyzeDailyLog(User user, String userText) {

        long start = System.currentTimeMillis();

        DailyMeal todayMeal = dailyMealService.getTodayMeal(user);
        String todayMealPrompt = buildTodayMealSection(todayMeal);

        String prompt = buildPromptV8(userText, todayMealPrompt);

        String geminiResponse = geminiClient.generateSmartJson(prompt);

        if (geminiResponse == null || geminiResponse.isBlank()) {
            return CompletableFuture.completedFuture(buildFallback(userText));
        }

        String json = extractJson(geminiResponse);

        try {
            DailyAnalysis result = objectMapper.readValue(json, DailyAnalysis.class);

            // replace → update 오탐 교정
            if ("replace".equalsIgnoreCase(result.getAction())) {
                boolean single = userText.matches(".*(아침|점심|저녁|간식).*");
                boolean fullReset = userText.matches(".*(전체|전부|다시|새로|처음부터|식단).*");
                if (single && !fullReset) {
                    result.setAction("update");
                }
            }

            // update인데 끼니 1개면 target 자동 추론
            if ("update".equalsIgnoreCase(result.getAction())
                    && result.getTargetMeal() == null
                    && result.getMeals() != null
                    && result.getMeals().size() == 1) {

                result.setTargetMeal(result.getMeals().get(0).getTime());
            }

            System.out.printf("✔ [Meal] 분석 완료 (%dms) → %s%n",
                    (System.currentTimeMillis() - start),
                    result.getAction());

            return CompletableFuture.completedFuture(result);

        } catch (Exception e) {
            return CompletableFuture.completedFuture(buildFallback(userText));
        }
    }

    /* ===========================================
       오늘 기존 식단
    =========================================== */
    private String buildTodayMealSection(DailyMeal todayMeal) {

        if (todayMeal == null || todayMeal.getMealsJson() == null) {
            return "(오늘은 아직 식단 기록이 없음)\n";
        }

        try {
            List<MealEntry> meals = objectMapper.readValue(
                    todayMeal.getMealsJson(),
                    new com.fasterxml.jackson.core.type.TypeReference<>() {}
            );

            if (meals.isEmpty()) return "(오늘은 아직 식단 기록이 없음)\n";

            StringBuilder sb = new StringBuilder("🔥 [오늘 기록된 식단]\n");

            for (MealEntry m : meals) {
                sb.append("- ").append(m.getTime()).append(": ");
                String foods = m.getFoods().stream()
                        .map(f -> f.getName() + "(" + f.getCalories() + "kcal)")
                        .collect(Collectors.joining(", "));
                sb.append(foods).append("\n");
            }

            return sb.toString();

        } catch (Exception e) {
            return "(오늘 식단 파싱 실패)\n";
        }
    }

    /* ===========================================
       Prompt v8 (영양정보 반드시 생성)
    =========================================== */
    private String buildPromptV8(String userText, String todayMealPrompt) {

        return """
너는 사용자의 '식단 기록'을 분석하는 전문 AI다.

⚠️ 반드시 아래 규칙을 지켜야 한다:
- JSON 이외의 글 출력 금지
- 코드블록 금지
- 설명 금지
- 단위는 항상 g
- 새 음식의 칼로리/탄단지는 반드시 새로 계산할 것
- 기존 기록의 칼로리를 복사하거나 그대로 가져오면 안됨

--------------------------------------------
📌 [오늘 기록된 식단]
""" + todayMealPrompt + """
--------------------------------------------
📌 [Action 규칙]
add: 그리고, 또, 추가, 더
update: 수정, 바꿔, 변경, 말고, 대신
delete: 빼, 제거, 삭제, 없애, 지워
replace: 다시, 처음부터, 전체, 전부(전체 교체)

--------------------------------------------
📌 targetMeal 규칙
- 끼니 1개 등장 → 해당 끼니
- 여러 끼니 등장 → null
- 끼니 언급 없음 → null
- update인데 meals=1개 → time으로 targetMeal 자동 추론

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
🟥 [절대 규칙 — 끼니(time)는 한국어만 사용]
--------------------------------------------
⚠️ "time" 필드는 절대로 영어(breakfast, lunch, dinner 등)로 출력하면 안 된다.
⚠️ 반드시 아래 한국어 중 하나만 사용해야 한다:

- "아침"
- "점심"
- "저녁"
- "간식"

--------------------------------------------
📥 입력 문장:
""" + userText;
    }

    private String extractJson(String text) {
        if (text == null) return "{}";
        int s = text.indexOf("{");
        int e = text.lastIndexOf("}");
        if (s >= 0 && e > s) return text.substring(s, e + 1).trim();
        return "{}";
    }

    private DailyAnalysis buildFallback(String userText) {
        return DailyAnalysis.builder()
                .action("error")
                .targetMeal(null)
                .meals(List.of())
                .totalCalories(0.0)
                .totalProtein(0.0)
                .totalFat(0.0)
                .totalCarbs(0.0)
                .message("AI 분석 실패: 다시 시도해주세요.")
                .build();
    }
}
