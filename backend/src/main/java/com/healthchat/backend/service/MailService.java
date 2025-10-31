package com.healthchat.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;
    private final StringRedisTemplate redisTemplate;

    /**
     * ✅ 1. 인증 코드 생성 및 이메일 전송
     */
    public void sendVerificationCode(String to) {
        // 6자리 인증 코드 생성
        String code = UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        // Redis에 저장 (5분 유효)
        String key = "verify:" + to;
        redisTemplate.opsForValue().set(key, code, 5, TimeUnit.MINUTES);

        // 이메일 내용 작성
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("[HealthChat+] 이메일 인증 코드");
        message.setText("""
                안녕하세요 😊
                HealthChat+ 이메일 인증 안내입니다.

                아래 인증 코드를 입력해주세요:
                ▶ 인증코드: %s

                (유효시간: 5분)
                """.formatted(code));

        // 이메일 발송
        mailSender.send(message);
    }

    /**
     * ✅ 2. 인증 코드 검증
     */
    public boolean verifyCode(String email, String inputCode) {
        String key = "verify:" + email;
        String storedCode = redisTemplate.opsForValue().get(key);

        if (storedCode != null && storedCode.equals(inputCode)) {
            redisTemplate.delete(key); // 검증 완료 시 제거
            return true;
        }
        return false;
    }
}

