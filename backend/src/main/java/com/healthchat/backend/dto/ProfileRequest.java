package com.healthchat.backend.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter @Setter
public class ProfileRequest {
    private String nickname;
    private String gender;
    private LocalDate birthDate;

    private Double height;
    private Double weight;
    private Double bodyFat;
    private Double goalWeight;
    private Double avgSleep;
    private Double sleepGoal;

    private String allergiesText;
    private String medicationsText;

    private String goalsDetailJson;
    private String goalText;
}
