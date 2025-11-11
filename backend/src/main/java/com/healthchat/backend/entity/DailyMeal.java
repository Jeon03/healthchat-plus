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
@Table(name = "daily_meal")
public class DailyMeal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate date;

    @Column(columnDefinition = "TEXT")
    private String mealsJson; // 전체 meal 구조를 JSON 문자열로 저장

    private Double totalCalories;
    private Double totalProtein;
    private Double totalFat;
    private Double totalCarbs;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}
