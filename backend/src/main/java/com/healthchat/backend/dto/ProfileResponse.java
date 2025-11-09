package com.healthchat.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter @Setter @Builder
public class ProfileResponse {
    private String nickname;
    private String gender;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate birthDate;
    private Integer age;

    private Double height;
    private Double weight;
    private Double bmi;

    private String allergiesText;
    private String medicationsText;
    private Double goalWeight;
    private Double avgSleep;

    /** ✅ 목표 관련 (GoalModal 연동) */
    private String goalsDetailJson;  // ex) [{"goal":"체중 감량","factors":["시간 부족","식탐"]}]
    private String goalText;         // ex) "꾸준히 운동해서 체중 감량과 스트레스 관리"
}
