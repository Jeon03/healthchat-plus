package com.healthchat.backend.controller;

import com.healthchat.backend.dto.AiCoachFeedbackDto;

import com.healthchat.backend.security.CustomUserDetails;
import com.healthchat.backend.service.AiCoachService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/coach")
@RequiredArgsConstructor
public class AiCoachFeedbackController {

    private final AiCoachService aiCoachFeedbackService;

    @GetMapping("/daily")
    public AiCoachFeedbackDto getDailyFeedback(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        if (user == null)
            throw new RuntimeException("로그인이 필요합니다.");

        // 날짜 없으면 오늘 기준
        LocalDate targetDate = (date != null ? date : LocalDate.now());

        return aiCoachFeedbackService.generateDailyFeedback(
                user.getId(),
                targetDate
        );
    }
}
