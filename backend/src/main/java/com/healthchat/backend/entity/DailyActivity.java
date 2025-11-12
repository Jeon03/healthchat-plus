package com.healthchat.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "daily_activities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ✅ 사용자 연결
    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    // ✅ 날짜 (unique per user)
    @Column(nullable = false)
    private LocalDate date;

    // ✅ 통계 필드
    private double totalCalories;
    private double totalDuration; // 분 단위

    // ✅ 하위 운동 목록
    @OneToMany(mappedBy = "activity", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ExerciseItem> exercises = new ArrayList<>();

    public void addExercise(ExerciseItem item) {
        item.setActivity(this);
        exercises.add(item);
    }
}
