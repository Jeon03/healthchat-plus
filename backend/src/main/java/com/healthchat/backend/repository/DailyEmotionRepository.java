package com.healthchat.backend.repository;

import com.healthchat.backend.entity.DailyEmotion;
import com.healthchat.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface DailyEmotionRepository extends JpaRepository<DailyEmotion, Long> {


    Optional<DailyEmotion> findByUserAndDate(User user, LocalDate date);
    
    void deleteByUserAndDate(User user, LocalDate date);

}
