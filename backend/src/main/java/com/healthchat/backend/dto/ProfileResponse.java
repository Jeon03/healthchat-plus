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

    private Double bodyFat;
    private String allergiesText;
    private String medicationsText;
    private Double goalWeight;
    private Double sleepGoal;
    private Double avgSleep;
}
