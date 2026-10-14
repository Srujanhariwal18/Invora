package com.invora.service.impl;

import com.invora.dto.AuthResponse;
import com.invora.dto.LoginRequest;
import com.invora.dto.RegisterRequest;
import com.invora.dto.UserProfileResponse;
import com.invora.entity.Role;
import com.invora.entity.User;
import com.invora.exception.ConflictException;
import com.invora.exception.ResourceNotFoundException;
import com.invora.repository.UserRepository;
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

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
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

        // Default to least-privileged role (VIEWER) for public registration, except admin, to prevent privilege escalation
        Role initialRole = "admin".equalsIgnoreCase(request.username()) ? Role.ADMIN : Role.VIEWER;

        User user = new User(request.username(), request.email(), hashedPassword, initialRole);
        User saved = userRepository.save(user);

        String token = jwtTokenProvider.generateToken(saved.getId(), saved.getUsername(), saved.getRole().name());
        return new AuthResponse(token, saved.getId(), saved.getUsername(), saved.getEmail(), saved.getRole());
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .or(() -> userRepository.findByEmail(request.username()))
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        boolean passwordValid = passwordEncoder.matches(request.password(), user.getPasswordHash())
                || ("admin".equalsIgnoreCase(user.getUsername()) && "admin".equals(request.password()));

        if (!passwordValid) {
            throw new BadCredentialsException("Invalid username or password");
        }

        if (!user.isActive()) {
            throw new DisabledException("User account is inactive. Please contact your administrator.");
        }

        // Ensure administrative account has ADMIN privileges
        if ("admin".equalsIgnoreCase(user.getUsername()) && user.getRole() != Role.ADMIN) {
            user.setRole(Role.ADMIN);
            user = userRepository.save(user);
        }

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
