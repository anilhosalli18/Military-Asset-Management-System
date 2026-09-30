package com.mams.service.impl;

import com.mams.dto.user.CreateUserRequest;
import com.mams.dto.user.ResetPasswordRequest;
import com.mams.dto.user.UpdateUserRequest;
import com.mams.dto.user.UserDTO;
import com.mams.exception.BadRequestException;
import com.mams.exception.ConflictException;
import com.mams.exception.ResourceNotFoundException;
import com.mams.exception.UnauthorizedBaseAccessException;
import com.mams.model.Base;
import com.mams.model.User;
import com.mams.model.enums.Role;
import com.mams.repository.BaseRepository;
import com.mams.repository.UserRepository;
import com.mams.security.UserPrincipal;
import com.mams.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final BaseRepository baseRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository,
                           BaseRepository baseRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.baseRepository = baseRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDTO createUser(CreateUserRequest request, Authentication auth) {
        validateAdmin(auth);

        if (request == null) {
            throw new BadRequestException("Create user request cannot be null");
        }

        // Validate uniqueness -> 409 Conflict
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("Username '" + request.getUsername() + "' is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email '" + request.getEmail() + "' is already registered");
        }

        // Validate password policy (minimum 8 characters)
        if (request.getPassword() == null || request.getPassword().length() < 8) {
            throw new BadRequestException("Password must be at least 8 characters in length");
        }

        // Validate role and base consistency
        Role role = request.getRole();
        if (role == null) {
            throw new BadRequestException("User role is required");
        }

        Base base = null;
        if (role == Role.ADMIN) {
            if (request.getBaseId() != null) {
                throw new BadRequestException("Admin role cannot be assigned to a specific base");
            }
        } else {
            if (request.getBaseId() == null) {
                throw new BadRequestException("Non-admin roles must be assigned to an active military base");
            }
            base = baseRepository.findById(request.getBaseId())
                    .filter(b -> Boolean.TRUE.equals(b.getIsActive()))
                    .orElseThrow(() -> new ResourceNotFoundException("Active base not found with ID: " + request.getBaseId()));
        }

        User user = User.builder()
                .username(request.getUsername().trim())
                .email(request.getEmail().trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName().trim())
                .role(role)
                .base(base)
                .isActive(true)
                .build();

        User saved = userRepository.save(user);
        return mapToDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserDTO> getUsers(String roleStr, Long baseId, Boolean isActive, Pageable pageable, Authentication auth) {
        validateAdmin(auth);

        Role roleEnum = null;
        if (roleStr != null && !roleStr.trim().isEmpty()) {
            try {
                roleEnum = Role.valueOf(roleStr.trim().toUpperCase());
            } catch (Exception ignored) {
                // Unknown role string defaults to null filter
            }
        }

        Pageable effectivePageable = pageable;
        if (pageable == null || pageable.getSort().isUnsorted()) {
            int page = pageable != null ? pageable.getPageNumber() : 0;
            int size = pageable != null ? pageable.getPageSize() : 10;
            effectivePageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        }

        Page<User> users = userRepository.findUsersPaged(roleEnum, baseId, isActive, effectivePageable);
        return users.map(this::mapToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDTO getUserById(Long id, Authentication auth) {
        validateAdmin(auth);

        User user = userRepository.findByIdWithBase(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));
        return mapToDTO(user);
    }

    @Override
    public UserDTO updateUser(Long id, UpdateUserRequest request, Authentication auth) {
        UserPrincipal admin = validateAdmin(auth);

        User user = userRepository.findByIdWithBase(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));

        // Prevent self-lockout: Admin cannot demote or deactivate their own account
        if (admin.getId().equals(id)) {
            if (request.getIsActive() != null && !request.getIsActive()) {
                throw new BadRequestException("Administrators cannot deactivate their own account");
            }
            if (request.getRole() != null && request.getRole() != Role.ADMIN) {
                throw new BadRequestException("Administrators cannot demote their own account");
            }
        }

        // Role & Base consistency validation
        Role newRole = request.getRole();
        if (newRole == Role.ADMIN) {
            if (request.getBaseId() != null) {
                throw new BadRequestException("Admin role cannot be assigned to a specific base");
            }
            user.setBase(null);
        } else {
            if (request.getBaseId() == null) {
                throw new BadRequestException("Non-admin roles must be assigned to an active military base");
            }
            Base base = baseRepository.findById(request.getBaseId())
                    .filter(b -> Boolean.TRUE.equals(b.getIsActive()))
                    .orElseThrow(() -> new ResourceNotFoundException("Active base not found with ID: " + request.getBaseId()));
            user.setBase(base);
        }

        user.setFullName(request.getFullName().trim());
        user.setRole(newRole);
        user.setIsActive(request.getIsActive());

        User updated = userRepository.save(user);
        return mapToDTO(updated);
    }

    @Override
    public void deactivateUser(Long id, Authentication auth) {
        UserPrincipal admin = validateAdmin(auth);

        if (admin.getId().equals(id)) {
            throw new BadRequestException("Administrators cannot deactivate their own account");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));

        user.setIsActive(false);
        userRepository.save(user);
    }

    @Override
    public UserDTO resetPassword(Long id, ResetPasswordRequest request, Authentication auth) {
        validateAdmin(auth);

        if (request == null || request.getNewPassword() == null || request.getNewPassword().length() < 8) {
            throw new BadRequestException("New password must be at least 8 characters in length");
        }

        User user = userRepository.findByIdWithBase(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        User updated = userRepository.save(user);
        return mapToDTO(updated);
    }

    private UserPrincipal validateAdmin(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new BadRequestException("Authenticated administrator credentials required");
        }
        if (principal.getRole() != Role.ADMIN) {
            throw new UnauthorizedBaseAccessException("Access denied: Administrator privileges required");
        }
        return principal;
    }

    private UserDTO mapToDTO(User u) {
        return UserDTO.builder()
                .id(u.getId())
                .username(u.getUsername())
                .email(u.getEmail())
                .fullName(u.getFullName())
                .role(u.getRole())
                .baseId(u.getBase() != null ? u.getBase().getId() : null)
                .baseName(u.getBase() != null ? u.getBase().getName() : null)
                .isActive(u.getIsActive())
                .createdAt(null)
                .build();
    }
}
