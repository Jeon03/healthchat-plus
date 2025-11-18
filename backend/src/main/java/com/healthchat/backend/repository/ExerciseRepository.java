package com.healthchat.backend.repository;

import com.healthchat.backend.entity.ExerciseItem;
import com.healthchat.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface ExerciseRepository extends JpaRepository<ExerciseItem, Long> {

    @Modifying
    @Query("""
        DELETE FROM ExerciseItem e
        WHERE e.activity.user = :user
        AND e.activity.date = :date
    """)
    void deleteByUserAndDate(User user, LocalDate date);
}