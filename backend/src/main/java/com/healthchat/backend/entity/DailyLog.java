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
    @Column(nullable = false)
    private LocalDate date;

    // ✅ 연결된 하위 데이터
    @OneToOne(cascade = CascadeType.ALL)
    private DailyMeal meal;

//    @OneToOne(cascade = CascadeType.ALL)
//    private DailyActivity activity;
//
//    @OneToOne(cascade = CascadeType.ALL)
//    private DailyMood mood;

    // ✅ 요약용 필드
    private double totalCalories;
    private double totalExerciseTime; // 분 단위
    private String moodSummary;
}
