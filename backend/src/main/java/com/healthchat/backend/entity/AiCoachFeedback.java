// package 예시는 프로젝트 구조에 맞게 수정
package com.healthchat.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "ai_coach_feedback",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "date"})
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)  // JPA 필수
@AllArgsConstructor(access = AccessLevel.PRIVATE)   // Builder 전용
@Builder
public class AiCoachFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDate date;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String dietAdvice;

    @Column(columnDefinition = "TEXT")
    private String exerciseAdvice;

    @Column(columnDefinition = "TEXT")
    private String emotionAdvice;

    @Column(columnDefinition = "TEXT")
    private String goalAlignment;

    @Column(columnDefinition = "TEXT")
    private String referencesJson;

    private LocalDateTime createdAt;
}

