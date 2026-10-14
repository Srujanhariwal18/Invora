package com.invora.controller;

import com.invora.dto.UserProfileResponse;
import com.invora.entity.AlertStatus;
import com.invora.entity.User;
import com.invora.exception.ResourceNotFoundException;
import com.invora.repository.AlertRepository;
import com.invora.repository.UserRepository;
import com.invora.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Section 2: Normal Users Section Controller.
 * Accessible to authenticated normal users (ROLE_USER, STAFF, WAREHOUSE_MANAGER, VIEWER) and ADMIN.
 */
@RestController
@RequestMapping("/api/user")
@PreAuthorize("hasAnyRole('USER', 'ADMIN', 'STAFF', 'WAREHOUSE_MANAGER', 'VIEWER')")
public class UserController {

    private final UserRepository userRepository;
    private final AlertRepository alertRepository;

    public UserController(UserRepository userRepository, AlertRepository alertRepository) {
        this.userRepository = userRepository;
        this.alertRepository = alertRepository;
    }

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getUserProfile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = resolveCurrentUser(auth);
        return ResponseEntity.ok(UserProfileResponse.fromEntity(user));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getUserDashboard() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = resolveCurrentUser(auth);

        Map<String, Object> data = new HashMap<>();
        data.put("section", "NORMAL_USER");
        data.put("username", user.getUsername());
        data.put("email", user.getEmail());
        data.put("role", user.getRole().name());
        data.put("activeAlertsCount", alertRepository.countByStatus(AlertStatus.ACTIVE));
        data.put("message", "Welcome to Invora Warehouse Portal");
        return ResponseEntity.ok(data);
    }

    private User resolveCurrentUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            throw new org.springframework.security.authentication.BadCredentialsException("Authentication required");
        }
        if (auth.getPrincipal() instanceof UserPrincipal principal) {
            return userRepository.findById(principal.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId()));
        }
        String username = auth.getName();
        return userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
    }
}
