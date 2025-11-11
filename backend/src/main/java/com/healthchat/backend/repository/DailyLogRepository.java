package com.healthchat.backend.repository;

import com.healthchat.backend.entity.DailyLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface DailyLogRepository extends JpaRepository<DailyLog, Long> {
    Optional<DailyLog> findByUserIdAndDate(Long userId, LocalDate date);
}
