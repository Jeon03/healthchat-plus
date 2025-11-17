package com.healthchat.backend.controller;

import com.healthchat.backend.dto.AiCoachFeedbackDto;
import com.healthchat.backend.entity.AiCoachFeedback;
import com.healthchat.backend.security.CustomUserDetails;
import com.healthchat.backend.service.AiCoachFeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/coach")
@RequiredArgsConstructor
public class AiCoachFeedbackController {

    private final AiCoachFeedbackService feedbackService;

    /**
     * 🔍 오늘 피드백 조회 (DB에서만 조회)
     *   - 존재하면 반환
     *   - 없으면 null 반환 (자동 생성 ❌)
     */
    @GetMapping("/daily")
    public AiCoachFeedbackDto getDailyFeedback(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        if (user == null)
            throw new RuntimeException("로그인이 필요합니다.");

        LocalDate targetDate = (date != null ? date : LocalDate.now());

        return feedbackService.findDailyFeedback(
                user.getId(),
                targetDate
        );
    }

    /**
     * 🔄 버튼을 눌러 강제로 피드백 생성하는 API
     *   - 기존 데이터 무시하고 신규 생성
     */
    @PostMapping("/daily/generate")
    public AiCoachFeedbackDto regenerateFeedback(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        if (user == null)
            throw new RuntimeException("로그인이 필요합니다.");

        LocalDate targetDate = (date != null ? date : LocalDate.now());

        return feedbackService.generate(
                user.getId(),
                targetDate
        );
    }
    @GetMapping("/{date}")
    public ResponseEntity<?> getFeedbackByDate(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable String date
    ) {
        LocalDate targetDate = LocalDate.parse(date);

        AiCoachFeedbackDto dto = feedbackService.getByDate(user.getId(), targetDate);

        if (dto == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(dto);
    }
}
