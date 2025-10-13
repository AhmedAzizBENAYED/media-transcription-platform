package com.ahmedaziz.mediatranscriptionplatform.controller;

import com.ahmedaziz.mediatranscriptionplatform.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
public class HealthCheckController {

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, String>>> healthCheck() {
        Map<String, String> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "Media Transcription Platform");
        health.put("version", "1.0.0");

        return ResponseEntity.ok(ApiResponse.success(health, "Service is healthy"));
    }
}
