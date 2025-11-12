package com.healthchat.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ✅ 사용자 연결
    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    // ✅ 날짜 (Unique per user)
    @Column(nullable = false, unique = true)
    private LocalDate date;

    // ✅ 하루 식단 (섭취)
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "meal_id")
    private DailyMeal meal;

    // ✅ 하루 운동 (소모)
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "activity_id")
    private DailyActivity activity;

//    // ✅ 하루 감정 (심리 상태)
//    @OneToOne(cascade = CascadeType.ALL)
//    @JoinColumn(name = "mood_id")
//    private DailyMood mood;

    // ✅ 요약용 통계 필드
    private double totalCalories;       // 섭취 - 소모
    private double totalExerciseTime;   // 분 단위
    private String moodSummary;
}
