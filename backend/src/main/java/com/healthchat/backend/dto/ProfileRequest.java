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
    private String allergiesText;
    private String medicationsText;
    private Double goalWeight;
    private Double sleepGoal;
    private Double avgSleep;
}
