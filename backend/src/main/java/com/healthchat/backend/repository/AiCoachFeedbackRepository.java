package com.healthchat.backend.repository;

import com.healthchat.backend.entity.AiCoachFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface AiCoachFeedbackRepository extends JpaRepository<AiCoachFeedback, Long> {

    Optional<AiCoachFeedback> findByUserIdAndDate(Long userId, LocalDate date);
}
