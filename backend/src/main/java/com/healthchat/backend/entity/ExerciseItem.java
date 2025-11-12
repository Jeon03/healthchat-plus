package com.healthchat.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "exercise_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExerciseItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ✅ 상위 DailyActivity 연결
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id")
    private DailyActivity activity;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private ExerciseCategory category; // HELTH, CARDIO, YOGA, PILATES, STRETCHING, OTHER

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private BodyPart part; // CHEST, SHOULDER, BACK, ABS, LOWER, FULL, OTHER

    private String name; // 운동 이름 (벤치프레스, 스쿼트 등)
    private int durationMin; // 분 단위
    private int calories; // kcal

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private Intensity intensity; // LOW, MEDIUM, HIGH

    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
