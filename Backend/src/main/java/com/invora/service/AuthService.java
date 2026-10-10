package com.invora.service;

import com.invora.dto.AuthResponse;
import com.invora.dto.LoginRequest;
import com.invora.dto.RegisterRequest;
import com.invora.dto.UserProfileResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    UserProfileResponse getCurrentUserProfile();
}
