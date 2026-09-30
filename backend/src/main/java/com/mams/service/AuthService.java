package com.mams.service;

import com.mams.dto.auth.JwtAuthResponse;
import com.mams.dto.auth.LoginRequest;
import com.mams.dto.auth.UserDto;
import com.mams.security.UserPrincipal;

public interface AuthService {
    JwtAuthResponse login(LoginRequest loginRequest);
    UserDto getCurrentUser(UserPrincipal principal);
}
