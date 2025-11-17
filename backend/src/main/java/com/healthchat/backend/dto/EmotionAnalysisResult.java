package com.healthchat.backend.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmotionAnalysisResult {


    private String action;
    /** 여러 감정 리스트 */
    private List<String> emotions;        // ["스트레스", "행복"]

    /** 감정별 강도 점수 */
    private List<Integer> scores;         // [80, 60]

    /** 감정별 요약 */
    private List<String> summaries;       // ["과제 때문에 스트레스", "해방감으로 행복"]

    /** 감정별 키워드 리스트 */
    private List<List<String>> keywords;  // [["과제","압박"], ["여유","기쁨"]]

    /** 대표 감정 */
    private String primaryEmotion;        // "행복"

    /** 대표 감정 점수 */
    private int primaryScore;             // 60

    /** 원문 텍스트 */
    private String rawText;

    /** 🔥 삭제 응답 */
    public static EmotionAnalysisResult deleted() {
        return EmotionAnalysisResult.builder()
                .action("delete")
                .emotions(List.of())
                .scores(List.of())
                .summaries(List.of())
                .keywords(List.of())
                .primaryEmotion(null)
                .primaryScore(0)
                .rawText(null)
                .build();
    }
}