package com.healthchat.backend.repository;

import com.healthchat.backend.entity.ExerciseItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ExerciseRepository extends JpaRepository<ExerciseItem, Long> {
    List<ExerciseItem> findByActivity_User_IdAndActivity_Date(Long userId, LocalDate date);
}