package com.healthchat.backend.repository;

import com.healthchat.backend.entity.DailyMeal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface DailyMealRepository extends JpaRepository<DailyMeal, Long> {
    Optional<DailyMeal> findByUserIdAndDate(Long userId, LocalDate date);
}
