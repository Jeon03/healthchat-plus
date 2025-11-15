package com.healthchat.backend.repository;

import com.healthchat.backend.entity.GuidelineChunk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GuidelineChunkRepository extends JpaRepository<GuidelineChunk, Long> {

    List<GuidelineChunk> findBySourceOrderByChunkIndex(String source);

    boolean existsBySource(String source);
}
