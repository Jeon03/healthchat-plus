package com.healthchat.backend.repository;

import com.healthchat.backend.entity.DailyActivity;
import com.healthchat.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface DailyActivityRepository extends JpaRepository<DailyActivity, Long> {
    Optional<DailyActivity> findByUserAndDate(User user, LocalDate date);
}
