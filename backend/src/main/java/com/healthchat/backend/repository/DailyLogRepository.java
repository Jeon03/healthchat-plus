package com.healthchat.backend.repository;

import com.healthchat.backend.entity.DailyLog;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Optional;

public interface DailyLogRepository extends JpaRepository<DailyLog, Long> {
    Optional<DailyLog> findByUserIdAndDate(Long userId, LocalDate date);
    @Modifying
    @Query("update DailyLog d set d.activity = null where d.user.id = :userId and d.date = :date")
    void clearActivity(@Param("userId") Long userId, @Param("date") LocalDate date);

}
