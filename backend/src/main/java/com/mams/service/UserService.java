package com.mams.service;

import com.mams.dto.user.CreateUserRequest;
import com.mams.dto.user.ResetPasswordRequest;
import com.mams.dto.user.UpdateUserRequest;
import com.mams.dto.user.UserDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

public interface UserService {
    UserDTO createUser(CreateUserRequest request, Authentication auth);
    Page<UserDTO> getUsers(String role, Long baseId, Boolean isActive, Pageable pageable, Authentication auth);
    UserDTO getUserById(Long id, Authentication auth);
    UserDTO updateUser(Long id, UpdateUserRequest request, Authentication auth);
    void deactivateUser(Long id, Authentication auth);
    UserDTO resetPassword(Long id, ResetPasswordRequest request, Authentication auth);
}
