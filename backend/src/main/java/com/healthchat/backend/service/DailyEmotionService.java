package com.healthchat.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthchat.backend.dto.EmotionAnalysisResult;
import com.healthchat.backend.dto.EmotionSummaryDto;
import com.healthchat.backend.entity.DailyEmotion;
import com.healthchat.backend.entity.User;
import com.healthchat.backend.repository.DailyEmotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DailyEmotionService {

    private final DailyEmotionRepository emotionRepository;
    private final ObjectMapper mapper = new ObjectMapper();

    /* ==========================================================
     * 1) 조회
     * ========================================================== */
    public DailyEmotion getTodayEmotion(User user) {
        return emotionRepository.findByUserAndDate(user, LocalDate.now())
                .orElse(null);
    }

    public DailyEmotion getEmotionByDate(User user, LocalDate date) {
        return emotionRepository.findByUserAndDate(user, date)
                .orElse(null);
    }


    /* ==========================================================
     * 2) Gemini 기반 감정 저장 (다중 감정 구조)
     * ========================================================== */
    public DailyEmotion saveDailyEmotion(User user, EmotionAnalysisResult analysis) {

        LocalDate today = LocalDate.now();

        // 기존 or 새 엔티티
        DailyEmotion emotion = emotionRepository.findByUserAndDate(user, today)
                .orElse(DailyEmotion.builder()
                        .user(user)
                        .date(today)
                        .createdAt(LocalDateTime.now())
                        .build());

        if (analysis == null || analysis.getEmotions() == null || analysis.getEmotions().isEmpty()) {
            System.out.println("⚠️ 감정 분석 결과 없음 — 저장 안함");
            return emotion;
        }

        // 대표 감정
        emotion.setPrimaryEmotion(analysis.getPrimaryEmotion());
        emotion.setPrimaryScore(analysis.getPrimaryScore());

        // JSON 필드 저장
        emotion.setEmotionsJson(toJson(analysis.getEmotions()));
        emotion.setScoresJson(toJson(analysis.getScores()));
        emotion.setSummariesJson(toJson(analysis.getSummaries()));
        emotion.setKeywordsJson(toJson(analysis.getKeywords()));

        // 원문
        emotion.setRawText(analysis.getRawText());
        emotion.setCreatedAt(LocalDateTime.now());

        return emotionRepository.save(emotion);
    }


    /* ==========================================================
     * JSON 변환 함수
     * ========================================================== */
    private String toJson(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "[]";
        }
    }

    private <T> T fromJson(String json, Class<T> type) {
        try {
            return mapper.readValue(json, type);
        } catch (Exception e) {
            return null;
        }
    }


    /* ==========================================================
     * 3) 수동 감정 수정
     * ========================================================== */
    public DailyEmotion saveOrUpdateManual(User user, DailyEmotion updated) {

        LocalDate date = updated.getDate() != null ? updated.getDate() : LocalDate.now();

        DailyEmotion emotion = emotionRepository.findByUserAndDate(user, date)
                .orElse(DailyEmotion.builder()
                        .user(user)
                        .date(date)
                        .build());

        emotion.setPrimaryEmotion(updated.getPrimaryEmotion());
        emotion.setPrimaryScore(updated.getPrimaryScore());

        emotion.setEmotionsJson(updated.getEmotionsJson());
        emotion.setScoresJson(updated.getScoresJson());
        emotion.setSummariesJson(updated.getSummariesJson());
        emotion.setKeywordsJson(updated.getKeywordsJson());

        emotion.setRawText(updated.getRawText());
        emotion.setCreatedAt(LocalDateTime.now());

        return emotionRepository.save(emotion);
    }


    /* ==========================================================
     * 4) Entity → DTO 변환 (Controller 대신 Service가 담당)
     * ========================================================== */
    public EmotionSummaryDto toSummaryDto(DailyEmotion e) {

        List<String> emotions = fromJson(e.getEmotionsJson(), List.class);
        List<Integer> scores = fromJson(e.getScoresJson(), List.class);
        List<String> summaries = fromJson(e.getSummariesJson(), List.class);
        List<List<String>> keywords = fromJson(e.getKeywordsJson(), List.class);

        return EmotionSummaryDto.builder()
                .primaryEmotion(e.getPrimaryEmotion())
                .primaryScore(e.getPrimaryScore())
                .emotions(emotions)
                .scores(scores)
                .summaries(summaries)
                .keywords(keywords)
                .rawText(e.getRawText())
                .date(e.getDate().toString())
                .build();
    }
}
