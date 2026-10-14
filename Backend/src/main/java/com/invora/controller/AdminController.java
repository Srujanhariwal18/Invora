package com.invora.controller;

import com.invora.dto.UserProfileResponse;
import com.invora.entity.Role;
import com.invora.entity.User;
import com.invora.exception.ResourceNotFoundException;
import com.invora.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Section 1: Admin Section Controller.
 * Exclusively restricted to administrators possessing ROLE_ADMIN.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;

    public AdminController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getAdminDashboard() {
        Map<String, Object> data = new HashMap<>();
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.findAll().stream().filter(User::isActive).count();

        data.put("section", "ADMIN");
        data.put("status", "HEALTHY");
        data.put("totalUsers", totalUsers);
        data.put("activeUsers", activeUsers);
        data.put("systemVersion", "Invora 1.0.0-SNAPSHOT");
        data.put("message", "Welcome to Admin Management Panel");
        return ResponseEntity.ok(data);
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserProfileResponse>> getAllUsers() {
        List<UserProfileResponse> users = userRepository.findAll().stream()
                .map(UserProfileResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(users);
    }

    @PatchMapping("/users/{id}/role")
    @Transactional
    public ResponseEntity<UserProfileResponse> updateUserRole(@PathVariable UUID id, @RequestParam Role role) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        user.setRole(role);
        User saved = userRepository.save(user);
        return ResponseEntity.ok(UserProfileResponse.fromEntity(saved));
    }

    @PatchMapping("/users/{id}/status")
    @Transactional
    public ResponseEntity<UserProfileResponse> updateUserStatus(@PathVariable UUID id, @RequestParam boolean active) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        user.setActive(active);
        User saved = userRepository.save(user);
        return ResponseEntity.ok(UserProfileResponse.fromEntity(saved));
    }
}
