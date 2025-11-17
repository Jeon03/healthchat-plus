package com.healthchat.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthchat.backend.dto.EmotionAnalysisResult;
import com.healthchat.backend.dto.EmotionSummaryDto;
import com.healthchat.backend.entity.DailyEmotion;
import com.healthchat.backend.entity.User;
import com.healthchat.backend.repository.DailyEmotionRepository;
import com.healthchat.backend.repository.DailyMealRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DailyEmotionService {

    private final DailyEmotionRepository emotionRepository;
    private final ObjectMapper mapper = new ObjectMapper();
    private final DailyLogService dailyLogService;

    @Transactional
    public void deleteToday(User user) {

        LocalDate today = LocalDate.now();

        // 1️⃣ DailyLog에서 감정 FK 먼저 제거
        dailyLogService.clearEmotion(user, today);

        // 2️⃣ 감정 테이블 삭제
        emotionRepository.deleteByUserAndDate(user, today);

        System.out.println("🗑 감정 기록 전체 삭제 완료");
    }

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
     * 2) Gemini 기반 감정 저장 (다중 감정 누적 저장)
     * ========================================================== */
    @Transactional
    public DailyEmotion saveDailyEmotion(User user, EmotionAnalysisResult analysis) {

        LocalDate today = LocalDate.now();

        // 오늘 감정 기록 조회
        DailyEmotion emotion = emotionRepository.findByUserAndDate(user, today)
                .orElse(DailyEmotion.builder()
                        .user(user)
                        .date(today)
                        .emotionsJson("[]")
                        .scoresJson("[]")
                        .summariesJson("[]")
                        .keywordsJson("[]")
                        .rawText("")
                        .createdAt(LocalDateTime.now())
                        .build()
                );

        if (analysis == null || analysis.getEmotions() == null || analysis.getEmotions().isEmpty()) {
            return emotion;
        }

    /* ---------------------------------------------------
       기존 JSON → 리스트 변환
       --------------------------------------------------- */
        List<String> prevEmotions = fromJsonList(emotion.getEmotionsJson());
        List<Integer> prevScores = fromJsonIntList(emotion.getScoresJson());
        List<String> prevSummaries = fromJsonList(emotion.getSummariesJson());
        List<List<String>> prevKeywords = fromJson2DList(emotion.getKeywordsJson());

    /* ---------------------------------------------------
       신규 감정 append
       --------------------------------------------------- */
        List<String> newEmotions = analysis.getEmotions();
        List<Integer> newScores = analysis.getScores();
        List<String> newSummaries = analysis.getSummaries();
        List<List<String>> newKeywords = analysis.getKeywords();

        for (int i = 0; i < newEmotions.size(); i++) {
            prevEmotions.add(newEmotions.get(i));
            prevScores.add(newScores.get(i));
            prevSummaries.add(newSummaries.get(i));
            prevKeywords.add(newKeywords.get(i));
        }

    /* ---------------------------------------------------
       대표 감정(primaryEmotion) 재계산
       --------------------------------------------------- */
        int maxIdx = 0;
        for (int i = 1; i < prevScores.size(); i++) {
            if (prevScores.get(i) > prevScores.get(maxIdx)) {
                maxIdx = i;
            }
        }
        emotion.setPrimaryEmotion(prevEmotions.get(maxIdx));
        emotion.setPrimaryScore(prevScores.get(maxIdx));

    /* ---------------------------------------------------
       JSON 저장
       --------------------------------------------------- */
        emotion.setEmotionsJson(toJson(prevEmotions));
        emotion.setScoresJson(toJson(prevScores));
        emotion.setSummariesJson(toJson(prevSummaries));
        emotion.setKeywordsJson(toJson(prevKeywords));

        /* rawText 이어붙이기 */
        String merged = (emotion.getRawText() == null ? "" : emotion.getRawText() + "\n")
                + analysis.getRawText();
        emotion.setRawText(merged);
        emotion.setCreatedAt(LocalDateTime.now());

        return emotionRepository.save(emotion);
    }


    /* ==========================================================
       JSON 파싱 유틸
       ========================================================== */
    private List<String> fromJsonList(String json) {
        try {
            return mapper.readValue(json, mapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (Exception e) {
            return new java.util.ArrayList<>();
        }
    }

    private List<Integer> fromJsonIntList(String json) {
        try {
            return mapper.readValue(json, mapper.getTypeFactory().constructCollectionType(List.class, Integer.class));
        } catch (Exception e) {
            return new java.util.ArrayList<>();
        }
    }

    private List<List<String>> fromJson2DList(String json) {
        try {
            return mapper.readValue(json,
                    mapper.getTypeFactory().constructCollectionType(List.class,
                            mapper.getTypeFactory().constructCollectionType(List.class, String.class)));
        } catch (Exception e) {
            return new java.util.ArrayList<>();
        }
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
