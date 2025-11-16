package com.healthchat.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthchat.backend.dto.AiCoachFeedbackDto;
import com.healthchat.backend.entity.AiCoachFeedback;
import com.healthchat.backend.entity.User;
import com.healthchat.backend.repository.AiCoachFeedbackRepository;
import com.healthchat.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiCoachFeedbackService {

    private final AiCoachFeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final AiCoachService aiCoachService;

    /**
     * 🔍 조회만 진행 — DB에 없으면 null 반환
     *    ✨ 자동 생성 없음!
     */
    public AiCoachFeedbackDto findDailyFeedback(Long userId, LocalDate date) {
        return feedbackRepository.findByUserIdAndDate(userId, date)
                .map(this::toDto)
                .orElse(null);
    }

    /**
     * 🔥 버튼 클릭 시 강제 생성 API
     *    - 기존 피드백 있으면 덮어씀
     */
    public AiCoachFeedbackDto generate(Long userId, LocalDate date) {

        AiCoachFeedbackDto aiResult = aiCoachService.generateDailyFeedback(userId, date);

        saveFeedback(userId, date, aiResult);

        return aiResult;
    }


    private void saveFeedback(Long userId, LocalDate date, AiCoachFeedbackDto dto) {

        User user = userRepository.findById(userId)
                .orElseThrow();

        AiCoachFeedback entity = feedbackRepository
                .findByUserIdAndDate(userId, date)
                .orElse(AiCoachFeedback.builder()
                        .user(user)
                        .date(date)
                        .createdAt(LocalDateTime.now())
                        .build()
                );

        entity.setSummary(dto.getSummary());
        entity.setDietAdvice(dto.getDietAdvice());
        entity.setExerciseAdvice(dto.getExerciseAdvice());
        entity.setEmotionAdvice(dto.getEmotionAdvice());
        entity.setGoalAlignment(dto.getGoalAlignment());

        try {
            entity.setReferencesJson(
                    dto.getReferences() != null
                            ? objectMapper.writeValueAsString(dto.getReferences())
                            : null
            );
        } catch (Exception e) {
            entity.setReferencesJson(null);
        }

        feedbackRepository.save(entity);
    }


    private AiCoachFeedbackDto toDto(AiCoachFeedback e) {
        AiCoachFeedbackDto dto = new AiCoachFeedbackDto();

        dto.setSummary(e.getSummary());
        dto.setDietAdvice(e.getDietAdvice());
        dto.setExerciseAdvice(e.getExerciseAdvice());
        dto.setEmotionAdvice(e.getEmotionAdvice());
        dto.setGoalAlignment(e.getGoalAlignment());

        try {
            if (e.getReferencesJson() != null) {
                var refs = objectMapper.readValue(
                        e.getReferencesJson(),
                        AiCoachFeedbackDto.Reference[].class
                );
                dto.setReferences(List.of(refs));
            }
        } catch (Exception ignored) {}

        return dto;
    }
}
