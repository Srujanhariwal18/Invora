package com.invora.service.impl;

import com.invora.dto.AuthResponse;
import com.invora.dto.LoginRequest;
import com.invora.dto.RegisterRequest;
import com.invora.dto.UserProfileResponse;
import com.invora.entity.Role;
import com.invora.entity.User;
import com.invora.entity.AuditLog;
import com.invora.exception.ConflictException;
import com.invora.exception.ResourceNotFoundException;
import com.invora.repository.UserRepository;
import com.invora.repository.AuditLogRepository;
import com.invora.security.JwtTokenProvider;
import com.invora.security.UserPrincipal;
import com.invora.service.AuthService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuditLogRepository auditLogRepository;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtTokenProvider jwtTokenProvider,
                           AuditLogRepository auditLogRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ConflictException("Username '" + request.username() + "' is already taken");
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email '" + request.email() + "' is already registered");
        }

        String hashedPassword = passwordEncoder.encode(request.password());

        User user = new User(request.username(), request.email(), hashedPassword, Role.VIEWER);
        User saved = userRepository.save(user);

        String token = jwtTokenProvider.generateToken(saved.getId(), saved.getUsername(), saved.getRole().name());
        return new AuthResponse(token, saved.getId(), saved.getUsername(), saved.getEmail(), saved.getRole());
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        Optional<User> userResult = userRepository.findByUsername(request.username())
                .or(() -> userRepository.findByEmail(request.username()));
        if (userResult.isEmpty()) {
            auditLogRepository.save(new AuditLog(null, "LOGIN_FAILED", "User", request.username(),
                    "Login failed for invalid credentials"));
            throw new BadCredentialsException("Invalid username or password");
        }
        User user = userResult.get();

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            auditLogRepository.save(new AuditLog(user, "LOGIN_FAILED", "User", user.getId().toString(),
                    "Login failed for invalid credentials"));
            throw new BadCredentialsException("Invalid username or password");
        }

        if (!user.isActive()) {
            auditLogRepository.save(new AuditLog(user, "LOGIN_FAILED", "User", user.getId().toString(),
                    "Login failed for inactive account"));
            throw new DisabledException("User account is inactive. Please contact your administrator.");
        }

        auditLogRepository.save(new AuditLog(user, "LOGIN", "User", user.getId().toString(),
                "User logged in successfully"));
        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole().name());
        return new AuthResponse(token, user.getId(), user.getUsername(), user.getEmail(), user.getRole());
    }

    @Override
    public UserProfileResponse getCurrentUserProfile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new BadCredentialsException("No authenticated user context found");
        }

        if (auth.getPrincipal() instanceof UserPrincipal principal) {
            return userRepository.findById(principal.getId())
                    .map(UserProfileResponse::fromEntity)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId()));
        }

        String username = auth.getName();
        User user = userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        return UserProfileResponse.fromEntity(user);
    }
}
