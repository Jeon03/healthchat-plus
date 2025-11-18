package com.healthchat.backend.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExerciseAnalysisResult {

    /** add | update | delete | replace | error */
    private String action;

    /** 분석된 운동 목록 */
    private List<ExerciseItemDto> exercises;

    /** 삭제 대상 운동 이름 목록 */
    private List<String> deleteTargets;

    /** 하루 총 소모 칼로리 */
    private double totalCalories;

    /** 하루 총 운동 시간 (분 단위) */
    private double totalDuration;

    /** 오류 시 메시지 */
    private String message;

    /** 🔥 전체 삭제 응답 */
    public static ExerciseAnalysisResult deleted() {
        return ExerciseAnalysisResult.builder()
                .action("delete")
                .exercises(List.of())
                .deleteTargets(List.of())   // 전체 삭제 → 대상 없음
                .totalCalories(0)
                .totalDuration(0)
                .build();
    }
}
