package com.healthchat.backend.repository;

import com.healthchat.backend.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {


    Optional<RefreshToken> findByEmail(String email);
    void deleteByEmail(String email);

}
