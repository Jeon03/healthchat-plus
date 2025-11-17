package com.healthchat.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "date"})
        }
)
public class DailyLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 사용자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    // 날짜
    @Column(nullable = false)
    private LocalDate date;

    // 하루 식단
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "meal_id")
    private DailyMeal meal;

    // 하루 운동
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "activity_id")
    private DailyActivity activity;

    // 하루 감정 → DailyEmotion 엔티티
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "emotion_id")
    private DailyEmotion emotion;

    // 요약 통계
    private double totalCalories;       // 섭취 - 소모
    private double totalExerciseTime;
    private String moodSummary;

    // AI 피드백 저장
    @Lob
    @Column(columnDefinition = "TEXT")
    private String aiCoachFeedback;
}

