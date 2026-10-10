package com.invora.controller;

import com.invora.dto.HealthResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> getHealth() {
        HealthResponse response = new HealthResponse(
                "UP",
                "Invora Warehouse Management System",
                "1.0.0",
                Instant.now()
        );
        return ResponseEntity.ok(response);
    }
}
