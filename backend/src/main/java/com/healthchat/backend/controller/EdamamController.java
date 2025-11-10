package com.healthchat.backend.controller;

import com.healthchat.backend.service.EdamamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/test")
public class EdamamController {

    private final EdamamService edamamService;

    /** ✅ 음식명 기반 영양정보 테스트용 */
    @GetMapping("/nutrition")
    public ResponseEntity<?> getNutrition(@RequestParam String food) {
        Map<String, Object> result = edamamService.getNutrition(food);
        return ResponseEntity.ok(result);
    }
}
