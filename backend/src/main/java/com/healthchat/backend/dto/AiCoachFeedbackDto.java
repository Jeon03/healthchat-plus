package com.healthchat.backend.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiCoachFeedbackDto {

    /** 전체 요약 */
    private String summary;

    /** 식단 관련 피드백 */
    private String dietAdvice;

    /** 운동 관련 피드백 */
    private String exerciseAdvice;

    /** 감정/스트레스 관련 피드백 */
    private String emotionAdvice;

    /** 사용자 목표와의 정렬도 / 조언 */
    private String goalAlignment;

    /** 참고 문헌 목록 (어떤 근거에서 말하는지) */
    private List<Reference> references;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Reference {
        private String source;   // 예: "kdr-2020", "who-activity"
        private String snippet;  // 근거 문장
        private String comment;  // 이걸 왜 인용했는지
    }
}
