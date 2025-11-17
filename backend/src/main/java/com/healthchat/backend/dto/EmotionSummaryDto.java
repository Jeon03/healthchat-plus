package com.healthchat.backend.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmotionSummaryDto {

    private String primaryEmotion;
    private int primaryScore;

    private List<String> emotions;
    private List<Integer> scores;
    private List<String> summaries;
    private List<List<String>> keywords;

    private String rawText;
    private String date;

    public static EmotionSummaryDto deleted() {
        return EmotionSummaryDto.builder()
                .primaryEmotion(null)
                .primaryScore(0)
                .emotions(List.of())
                .scores(List.of())
                .summaries(List.of())
                .keywords(List.of())
                .rawText(null)
                .date(null)
                .build();
    }
}
