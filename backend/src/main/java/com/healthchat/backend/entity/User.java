package com.healthchat.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ✅ 로그인용 이메일 (고유) */
    @Column(nullable = false, unique = true)
    private String email;

    /** ✅ 자체 회원가입용 비밀번호 (소셜 계정일 경우 null 가능) */
    private String password;

    /** ✅ 닉네임 */
    @Column(nullable = false)
    private String nickname;

    /** ✅ 로그인 제공자 (local, google, naver, kakao 등) */
    @Column(nullable = false)
    private String provider;

    /** ✅ 성별 (자체 회원가입 시 입력받음) */
    private String gender;

    /** ✅ 생년월일 (자체 회원가입 시 입력받음) */
    private LocalDate birthDate;

    /** ✅ 생성 시각 */
    @CreationTimestamp
    private LocalDateTime createdAt;


    /* =============================================
       ✅ HealthChat+ 프로필 확장 필드
       ============================================= */

    /** 키 (cm) */
    private Double height;

    /** 몸무게 (kg) */
    private Double weight;

    /** 체지방률 (%) */
    private Double bodyFat;

    /** 알레르기 정보 (자연어 그대로 저장) */
    @Column(columnDefinition = "TEXT")
    private String allergiesText;

    /** 복용 중인 약 정보 (자연어 그대로 저장) */
    @Column(columnDefinition = "TEXT")
    private String medicationsText;

    /** 목표 체중 (kg) */
    private Double goalWeight;

    /** 수면 목표 (시간) */
    private Double sleepGoal;

    /** 평균 수면 (시간) */
    private Double avgSleep;
}
