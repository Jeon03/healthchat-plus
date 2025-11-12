package com.healthchat.backend.dto;


import com.healthchat.backend.entity.BodyPart;
import com.healthchat.backend.entity.ExerciseCategory;
import com.healthchat.backend.entity.Intensity;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExerciseItemDto {
    private ExerciseCategory category;  // HELTH, CARDIO, YOGA, ...
    private BodyPart part;              // CHEST, BACK, LOWER, ...
    private String name;                // 운동 이름 (예: 벤치프레스)
    private int durationMin;            // 운동 시간(분)
    private Intensity intensity;        // LOW, MEDIUM, HIGH
    private int calories;               // 소모 칼로리
}