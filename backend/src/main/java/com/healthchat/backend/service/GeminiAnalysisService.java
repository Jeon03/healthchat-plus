package com.healthchat.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthchat.backend.config.GeminiClient;
import com.healthchat.backend.dto.DailyAnalysis;
import com.healthchat.backend.dto.FoodItem;
import com.healthchat.backend.dto.MealEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 🧠 GeminiAnalysisService
 * 사용자의 일기형 텍스트를 구조화된 식단 JSON으로 변환하는 서비스 (안정형)
 */
@Service
@RequiredArgsConstructor
public class GeminiAnalysisService {

    private final GeminiClient geminiClient; // Gemini API 호출 유틸
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 사용자의 하루 일기를 분석해 DailyAnalysis로 변환
     */
    public DailyAnalysis analyzeDailyLog(String userText) {
        String prompt = buildPrompt(userText);
        String geminiResponse = geminiClient.generateJson(prompt); // Gemini 호출

        // ✅ Gemini 서버 오류 시 null 반환됨
        if (geminiResponse == null) {
            System.out.println("⚠️ Gemini 서버 응답 없음 — 사용자 입력만 임시 분석으로 처리");
            return buildFallbackAnalysis(userText);
        }

        // ✅ Gemini가 문장 형식으로 JSON을 감싼 경우, JSON 부분만 추출
        String json = extractJson(geminiResponse);

        try {
            return objectMapper.readValue(json, DailyAnalysis.class);
        } catch (Exception e) {
            System.err.println("❌ Gemini JSON 파싱 실패: " + e.getMessage());
            System.err.println("⚠️ 응답 내용: " + geminiResponse);
            return buildFallbackAnalysis(userText); // ✅ 안전하게 fallback 반환
        }
    }

    /**
     * Gemini에 전달할 프롬프트 (단위 변환 강화)
     */
    private String buildPrompt(String userText) {
        return """
    너는 '자연어 식단 기록'을 구조화된 JSON으로 변환하는 영양 분석기야.

    입력은 사용자의 하루 식사 내용이야.
    이를 분석해서 아래 JSON 형식으로 출력해.

    {
      "meals": [
        {
          "time": "breakfast | lunch | dinner | snack",
          "foods": [
            {"name": "음식 이름(영문)", "quantity": (숫자), "unit": "g | bowl | piece | serving"}
          ]
        }
      ]
    }

    ⚖️ 단위 환산 기준표:
    - 밥 1공기 ≈ 210g
    - 라면 1봉지 ≈ 120g  
    - 국 1그릇 ≈ 300g  
    - 김치 1접시 ≈ 80g  
    - 고기 1인분 ≈ 150g  
    - 달걀 1개 ≈ 50g  
    - 우유 1컵 ≈ 200g  
    - 빵 1조각 ≈ 40g  

    규칙:
    - 수량이나 단위를 위 기준으로 추정해 g 단위로 변환
    - 과도하게 많은 양(예: 1000g 이상)은 피함
    - "그릇", "봉지", "공기", "컵", "조각" 등은 위 기준표를 참조
    - 다른 정보(운동, 감정)는 무시하고 오직 식단만 반환

    입력:
    """ + userText;
    }

    /**
     * Gemini 응답 문자열에서 JSON만 추출
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
     * ✅ Gemini 실패 시 — 사용자 입력을 유지한 기본 구조 반환
     */
    private DailyAnalysis buildFallbackAnalysis(String userText) {
        return DailyAnalysis.builder()
                .meals(List.of(
                        new MealEntry("unknown", List.of(
                                new FoodItem(userText, 0, "unknown", 0.0, 0.0, 0.0, 0.0)
                        ))
                ))
                .totalCalories(0.0)
                .totalProtein(0.0)
                .totalFat(0.0)
                .totalCarbs(0.0)
                .build();
    }
}
