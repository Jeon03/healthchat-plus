package com.healthchat.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthchat.backend.config.GeminiClient;
import com.healthchat.backend.dto.ExerciseAnalysisResult;
import com.healthchat.backend.entity.ExerciseItem;
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

    @Async
    public CompletableFuture<ExerciseAnalysisResult> analyzeExercise(
            Long userId,
            String userText,
            List<ExerciseItem> todayExercises
    ) {

        long start = System.currentTimeMillis();

        // 1) 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 2) 프롬프트 생성
        String prompt = buildPrompt(user, userText, todayExercises);

        // 3) Gemini 호출 (pro → flash 자동 fallback)
        String response = geminiClient.generateSmartJson(prompt);

        if (response == null || response.isBlank()) {
            System.out.println("⚠️ Gemini 응답 없음 — fallback 사용");
            return CompletableFuture.completedFuture(fallback());
        }

        // 4) JSON만 추출
        String json = extractJson(response);

        try {
            // 5) JSON → DTO 매핑
            ExerciseAnalysisResult result =
                    objectMapper.readValue(json, ExerciseAnalysisResult.class);
            System.out.println("🏋️‍♀️ Exercise JSON 결과 = " + json);
            long took = System.currentTimeMillis() - start;

            int duration = 0;
            try {
                duration = (int) Math.round(result.getTotalDuration());
            } catch (Exception ignore) {}

            System.out.printf(
                    "✅ [Exercise] 운동 분석 완료: action=%s (%.0f kcal, %d분) — %dms%n",
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


    /**
     * 오늘 운동 목록 → 문자열 포맷
     */
    private String formatExerciseList(List<ExerciseItem> list) {
        if (list == null || list.isEmpty()) return "없음";

        StringBuilder sb = new StringBuilder();
        for (ExerciseItem e : list) {
            sb.append("- ").append(e.getName())
                    .append(" / ").append(e.getDurationMin()).append("분")
                    .append(" / ").append(e.getCalories()).append("kcal\n");
        }
        return sb.toString();
    }

    private String buildPrompt(User user, String userText, List<ExerciseItem> todayExercises) {

        String gender = safe(user.getGender());
        double height = safeDouble(user.getHeight());
        double weight = safeDouble(user.getWeight());
        double bmi = (height > 0 && weight > 0)
                ? weight / Math.pow(height / 100.0, 2)
                : 0.0;
        int age = calculateAge(user.getBirthDate());
        String goal = safe(user.getGoalText());

        // ✅ 기존 오늘 운동 목록 변환
        String todayExerciseInfo = formatExerciseList(todayExercises);

        String profileInfo =
                "- 성별: " + gender + "\n" +
                        "- 나이: " + age + "세\n" +
                        "- 키: " + height + "cm\n" +
                        "- 몸무게: " + weight + "kg\n" +
                        "- BMI: " + String.format("%.1f", bmi) + "\n" +
                        "- 목표: " + goal + "\n";

        return """
너는 사용자의 운동 기록을 구조화하여 JSON으로 변환하는 운동 분석 AI야.
문장에는 식단/감정 내용이 섞여 있을 수 있으므로 운동 관련 내용만 분석해야 한다.

------------------------------------------------------
🔥 현재 오늘 운동 기록 (수정/삭제/교체 판단에 반드시 사용해야 함)
""" + todayExerciseInfo + """

------------------------------------------------------
🔥 새로 입력된 문장 (이 내용을 분석하여 action, exercises, deleteTargets를 생성)
""" + userText + """

------------------------------------------------------
🔥 목적
- 사용자가 한 운동을 정확하게 추출하고
- action(add/update/delete/replace)을 정확히 판단하며
- deleteTargets에 삭제/교체해야 할 기존 운동 이름을 정확히 담고
- exercises에는 새롭게 추가되거나 수정/교체될 운동만 넣고
- 총 운동 시간/칼로리를 계산한다.

------------------------------------------------------
🧍 사용자 정보(칼로리 계산에 반드시 반영):
""" + profileInfo + """

------------------------------------------------------
📌 [운동 관련 문장 인식 규칙]
운동으로 판단:
- 걷기, 뛰기, 달리기, 조깅, 러닝
- 자전거, 사이클, 수영
- 푸시업, 스쿼트, 데드리프트, 벤치프레스, 플랭크 등
- ~~분 운동, ~~시간 운동
- 헬스, 웨이트, 근력운동, 유산소, 스트레칭

식단/감정 문장은 무시:
- 먹다, 마시다, 밥, 라면, 샐러드 등
- 기분, 우울, 행복, 스트레스 등 감정 단어

------------------------------------------------------
📌 [action 판단 규칙 - 반드시 준수]

🔄 replace (기존 운동 중 특정 운동을 다른 운동으로 교체)
키워드: “말고”, “대신”, “대체해줘”, “이 운동 대신”
조건:
- 반드시 [기존 운동 A → 새 운동 B] 구조여야 함
출력 예시:
{
  "action": "replace",
  "deleteTargets": ["조깅"],
  "exercises": [{ ... 러닝 ... }]
}

✏ update (운동의 시간·강도 등 일부 수정)
키워드: “시간 수정”, “칼로리 수정”, “강도만 바꿔”, “조금 줄여”, “조금 늘려”
조건:
- 기존 운동 이름이 같아야 함
- deleteTargets는 항상 []

➕ add (추가)
키워드: “그리고”, “또”, “추가로”, “더”, “1시간 더”
규칙:
- 기존 운동과 이름이 동일 → duration/calories 누적 의미
- 다른 이름 → 새 운동 생성
- deleteTargets는 항상 []

🗑 delete (삭제)
다음 표현이 포함되면 무조건 delete:
- "지워", "삭제", "없애", "빼줘", "제거해줘", "삭제해줘"
- "지워줘", "없애줘", "빼", "삭제 부탁해"
- "삭제하고 싶어", "지우고 싶어"

delete 규칙:
1) 특정 운동 삭제
"조깅 지워"
→ { action:"delete", deleteTargets:["조깅"], exercises:[] }

2) 전체 운동 삭제
"운동 다 지워", "전체 삭제"
→ { action:"delete", deleteTargets:[], exercises:[] }

delete일 때 add/update/replace 로 판단하면 안 된다.

------------------------------------------------------
📌 [deleteTargets 규칙 — 매우 중요]

- deleteTargets: 오늘 기록에서 제거할 기존 운동 이름들의 배열
- add/update → deleteTargets = []
- delete (특정 삭제) → deleteTargets = ["조깅"]
- delete (전체 삭제) → deleteTargets = []
- replace → 기존 운동 A는 deleteTargets, 새 운동 B는 exercises

------------------------------------------------------
📌 [중복 운동 처리 규칙 — 반드시 준수]

todayExercises 는 오늘 이미 기록된 운동 목록이다.

1) 오늘 기록과 동일한 운동 이름이 다시 등장하면
   → 새 항목 생성 금지
   → 기존 duration/calories 에 더한다(merge)

2) “또”, “추가로”, “더”, “1시간 더” → add-merge 의도

3) 이름이 다르면 새 운동(add)

4) 동일 운동 중복 생성은 절대 금지

⚠ durationMin 및 calories 규칙
- exercises[] 에는 “사용자가 새로 말한 추가 시간”만 넣는다.
- 전체 합계(totalDuration, totalCalories)는 서버에서 재계산되므로 대략적이어도 된다.

------------------------------------------------------
📦 [출력 JSON 형식 — 반드시 이 형태만 출력]

{
  "action": "add" | "update" | "delete" | "replace",
  "exercises": [
    {
      "category": "CARDIO" | "STRENGTH" | "PILATES" | "YOGA" | "STRETCHING" | "OTHER",
      "part": "FULL" | "CHEST" | "BACK" | "LOWER" | "ABS" | "SHOULDER" | "ARM" | "OTHER",
      "name": "운동 이름",
      "durationMin": 숫자,
      "intensity": "LOW" | "MEDIUM" | "HIGH",
      "calories": 숫자
    }
  ],
  "deleteTargets": [ "조깅", "스쿼트" ], 
  "totalCalories": 숫자,
  "totalDuration": 숫자
}

⚠ JSON 이외의 텍스트(설명, 말풍선, 마크다운)는 절대 출력하지 마라.
""";

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
        if (text == null) return "{}";
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
