package com.healthchat.backend.dto;

import com.healthchat.backend.entity.DailyActivity;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class DailyActivityResponseDto {
    private DailyActivity activity;
    private double recommendedBurn;
}
