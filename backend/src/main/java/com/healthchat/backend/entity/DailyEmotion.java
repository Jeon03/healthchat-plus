package com.healthchat.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "daily_emotion")
public class DailyEmotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /** 하루 단위 기록 */
    private LocalDate date;

    /** ⭐ 대표 감정 (가장 강한 감정, primaryEmotion) */
    private String primaryEmotion;

    /** ⭐ 대표 감정 점수 */
    private int primaryScore;

    /** ⭐ 감정 이름 배열 → JSON 저장 */
    @Column(columnDefinition = "json")
    private String emotionsJson;
    // 예: ["스트레스","행복","피곤"]

    /** ⭐ 점수 배열 → JSON 저장 */
    @Column(columnDefinition = "json")
    private String scoresJson;
    // 예: [80, 60, 40]

    /** ⭐ 감정 요약 배열 → JSON 저장 */
    @Column(columnDefinition = "json")
    private String summariesJson;
    // 예: ["과제 때문에 스트레스","끝내고 행복","피곤함"]

    /** ⭐ 키워드 배열 → JSON 저장 (2차원 배열) */
    @Column(columnDefinition = "json")
    private String keywordsJson;
    // 예: [["과제","압박"],["해방감"],["피곤","지침"]]

    /** 자연어 전체 문장 */
    @Lob
    private String rawText;

    private LocalDateTime createdAt;
}
