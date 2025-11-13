package com.healthchat.backend.service;

import com.healthchat.backend.config.GeminiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * ✨ 라우팅 전용 서비스
 * 사용자 문장을 → 식단/운동/감정별로 분리하는 역할
 */
@Service
@RequiredArgsConstructor
public class GeminiRoutingService {

    private final GeminiClient geminiClient;

    public static record RoutingResult(
            String mealText,
            String exerciseText,
            String emotionText
    ) {}

    public RoutingResult route(String userText) {

        String prompt = buildPrompt(userText);

        String response = geminiClient.generateJson("gemini-2.5-pro", prompt);

        if (response == null) {
            System.out.println("⚠️ Routing 응답 없음");
            return new RoutingResult("", "", "");
        }

        String json = extractJson(response);

        try {
            Map<String, Object> map = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(json, Map.class);

            String meal = map.getOrDefault("mealText", "").toString();
            String exercise = map.getOrDefault("exerciseText", "").toString();
            String emotion = map.getOrDefault("emotionText", "").toString();

            // 🔥 라우팅 로그 출력
            System.out.println("\n===== 🧩 Routing 결과 =====");
            System.out.println("📌 식단(Meal): " + (meal.isBlank() ? "(없음)" : meal));
            System.out.println("🏋 운동(Exercise): " + (exercise.isBlank() ? "(없음)" : exercise));
            System.out.println("💬 감정(Emotion): " + (emotion.isBlank() ? "(없음)" : emotion));
            System.out.println("============================\n");

            return new RoutingResult(meal, exercise, emotion);

        } catch (Exception e) {
            System.err.println("⚠ Routing JSON parsing failed: " + e.getMessage());
            System.err.println("⚠ 원문 Routing 응답: " + response);
            return new RoutingResult("", "", "");
        }
    }

    private String buildPrompt(String text) {
        return """
너는 한국어 건강 일기 문장을 [식단], [운동], [감정] 세 가지로 정확하게 분류하는 AI 라우터다.

반드시 아래 JSON 형식만 반환해야 한다.
설명, 말머리, 주석, 코드블록, 자연어 등은 절대로 넣지 마라.

{
  "mealText": "",
  "exerciseText": "",
  "emotionText": ""
}

🎯 분류 규칙 (강화됨)
- 식사/식단 관련 문장 → mealText  
- 운동/활동/소모 관련 문장 → exerciseText  
- 감정/기분/심리 상태 → emotionText  

🟥 다음과 같은 “명령/메타 표현”은 모두 무시한다:
"전체 수정할거야", "추가로", "분석해줘",  
"정리해줘", "기록할게", "수정하려고", "할거야",  
"있어", "했어", "좀", "조금", 등 메타적 표현들은 무시하고  
문장에서 실제 의미가 있는 사건만 추출한다.

🟦 운동 문장 강화 규칙:
다음 단어가 포함되면 반드시 exerciseText에 넣는다:
- 팔굽혀펴기, 푸쉬업, 스쿼트, 런지, 플랭크  
- 걷기, 달리기, 뛰기, 조깅, 계단 오르기  
- 자전거, 헬스, 웨이트  
- 요가, 필라테스, 스트레칭  

🟩 식단 문장 강화 규칙:
음식명/식사명(아침, 점심, 저녁, 간식 포함)이 있으면 mealText에 넣음.

📌 반환 형식:
무조건 위 JSON 형식 그대로 출력하라.
빈 항목은 "" 로 둔다. null은 사용하지 말 것.

📥 입력 문장:
"%s"
""".formatted(text);
    }


    private String extractJson(String text) {
        int s = text.indexOf("{");
        int e = text.lastIndexOf("}");
        if (s >= 0 && e > s) return text.substring(s, e + 1);
        return text;
    }
}
