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

    /** ✅ 성별 */
    private String gender;

    /** ✅ 생년월일 */
    private LocalDate birthDate;

    /** ✅ 생성 시각 */
    @CreationTimestamp
    private LocalDateTime createdAt;

    /** 키 (cm) */
    private Double height;

    /** 몸무게 (kg) */
    private Double weight;

    /** ✅ 목표 체중 (kg) */
    private Double goalWeight;

    /** ✅ 평균 수면 시간 (시간) */
    private Double avgSleep;

    /** ✅ 알레르기 정보 (자연어 그대로 저장) */
    @Column(columnDefinition = "TEXT")
    private String allergiesText;

    /** ✅ 복용 중인 약 정보 (자연어 그대로 저장) */
    @Column(columnDefinition = "TEXT")
    private String medicationsText;

    /** ✅ 사용자의 목표 + 세부 요인 구조 (JSON 형태) */
    @Column(columnDefinition = "TEXT")
    private String goalsDetailJson;

    /** ✅ 사용자가 직접 작성한 자유 목표 문장 */
    @Column(columnDefinition = "TEXT")
    private String goalText;
}
