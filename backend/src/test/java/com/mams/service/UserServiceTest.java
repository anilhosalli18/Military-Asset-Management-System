package com.mams.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mams.dto.user.CreateUserRequest;
import com.mams.dto.user.ResetPasswordRequest;
import com.mams.dto.user.UpdateUserRequest;
import com.mams.dto.user.UserDTO;
import com.mams.exception.BadRequestException;
import com.mams.exception.ConflictException;
import com.mams.exception.UnauthorizedBaseAccessException;
import com.mams.model.Base;
import com.mams.model.User;
import com.mams.model.enums.Role;
import com.mams.repository.BaseRepository;
import com.mams.repository.UserRepository;
import com.mams.security.UserPrincipal;
import com.mams.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BaseRepository baseRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, baseRepository, passwordEncoder);
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    private Authentication createAuth(Long userId, String username, Role role, Long baseId) {
        UserPrincipal principal = UserPrincipal.builder()
                .id(userId)
                .username(username)
                .role(role)
                .baseId(baseId)
                .active(true)
                .authorities(Collections.emptyList())
                .build();
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    @Test
    @DisplayName("1. Non-admin (BASE_COMMANDER or LOGISTICS_OFFICER) is rejected 403 on every method")
    void testNonAdminRejectedOnEveryMethod() {
        Authentication commanderAuth = createAuth(2L, "commander", Role.BASE_COMMANDER, 1L);
        Authentication logisticsAuth = createAuth(3L, "logistics", Role.LOGISTICS_OFFICER, 1L);

        CreateUserRequest createReq = CreateUserRequest.builder()
                .username("new_officer")
                .email("officer@mams.mil")
                .password("StrongPass123")
                .fullName("Lt. John Doe")
                .role(Role.LOGISTICS_OFFICER)
                .baseId(1L)
                .build();

        UpdateUserRequest updateReq = UpdateUserRequest.builder()
                .fullName("Updated Name")
                .role(Role.LOGISTICS_OFFICER)
                .baseId(1L)
                .isActive(true)
                .build();

        ResetPasswordRequest resetReq = ResetPasswordRequest.builder()
                .newPassword("NewStrongPass123")
                .build();

        // Check for BASE_COMMANDER
        assertThrows(UnauthorizedBaseAccessException.class, () -> userService.createUser(createReq, commanderAuth));
        assertThrows(UnauthorizedBaseAccessException.class, () -> userService.getUsers(null, null, null, PageRequest.of(0, 10), commanderAuth));
        assertThrows(UnauthorizedBaseAccessException.class, () -> userService.getUserById(5L, commanderAuth));
        assertThrows(UnauthorizedBaseAccessException.class, () -> userService.updateUser(5L, updateReq, commanderAuth));
        assertThrows(UnauthorizedBaseAccessException.class, () -> userService.deactivateUser(5L, commanderAuth));
        assertThrows(UnauthorizedBaseAccessException.class, () -> userService.resetPassword(5L, resetReq, commanderAuth));

        // Check for LOGISTICS_OFFICER
        assertThrows(UnauthorizedBaseAccessException.class, () -> userService.createUser(createReq, logisticsAuth));
        assertThrows(UnauthorizedBaseAccessException.class, () -> userService.getUsers(null, null, null, PageRequest.of(0, 10), logisticsAuth));
        assertThrows(UnauthorizedBaseAccessException.class, () -> userService.getUserById(5L, logisticsAuth));
        assertThrows(UnauthorizedBaseAccessException.class, () -> userService.updateUser(5L, updateReq, logisticsAuth));
        assertThrows(UnauthorizedBaseAccessException.class, () -> userService.deactivateUser(5L, logisticsAuth));
        assertThrows(UnauthorizedBaseAccessException.class, () -> userService.resetPassword(5L, resetReq, logisticsAuth));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("2. Creating a user with role=ADMIN and a non-null base_id is rejected (400)")
    void testCreatingAdminWithNonNullBaseIdRejected() {
        Authentication adminAuth = createAuth(1L, "admin", Role.ADMIN, null);

        CreateUserRequest request = CreateUserRequest.builder()
                .username("second_admin")
                .email("admin2@mams.mil")
                .password("AdminPass123")
                .fullName("Second Admin")
                .role(Role.ADMIN)
                .baseId(10L) // Admin cannot have base_id
                .build();

        when(userRepository.existsByUsername("second_admin")).thenReturn(false);
        when(userRepository.existsByEmail("admin2@mams.mil")).thenReturn(false);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                userService.createUser(request, adminAuth));

        assertTrue(ex.getMessage().contains("Admin role cannot be assigned to a specific base"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("3. Creating a user with role=BASE_COMMANDER and a null base_id is rejected (400)")
    void testCreatingCommanderWithNullBaseIdRejected() {
        Authentication adminAuth = createAuth(1L, "admin", Role.ADMIN, null);

        CreateUserRequest request = CreateUserRequest.builder()
                .username("new_cmdr")
                .email("cmdr@mams.mil")
                .password("CommanderPass123")
                .fullName("Col. Sanders")
                .role(Role.BASE_COMMANDER)
                .baseId(null) // Non-admin must have base_id
                .build();

        when(userRepository.existsByUsername("new_cmdr")).thenReturn(false);
        when(userRepository.existsByEmail("cmdr@mams.mil")).thenReturn(false);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                userService.createUser(request, adminAuth));

        assertTrue(ex.getMessage().contains("Non-admin roles must be assigned to an active military base"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("4. Duplicate username or email returns 409 Conflict")
    void testDuplicateUsernameOrEmailReturns409() {
        Authentication adminAuth = createAuth(1L, "admin", Role.ADMIN, null);

        CreateUserRequest reqDuplicateUser = CreateUserRequest.builder()
                .username("existing_user")
                .email("unique@mams.mil")
                .password("Password123")
                .fullName("Duplicate User")
                .role(Role.ADMIN)
                .build();

        when(userRepository.existsByUsername("existing_user")).thenReturn(true);

        ConflictException exUser = assertThrows(ConflictException.class, () ->
                userService.createUser(reqDuplicateUser, adminAuth));
        assertTrue(exUser.getMessage().contains("Username 'existing_user' is already taken"));

        CreateUserRequest reqDuplicateEmail = CreateUserRequest.builder()
                .username("brand_new_user")
                .email("existing@mams.mil")
                .password("Password123")
                .fullName("New User")
                .role(Role.ADMIN)
                .build();

        when(userRepository.existsByUsername("brand_new_user")).thenReturn(false);
        when(userRepository.existsByEmail("existing@mams.mil")).thenReturn(true);

        ConflictException exEmail = assertThrows(ConflictException.class, () ->
                userService.createUser(reqDuplicateEmail, adminAuth));
        assertTrue(exEmail.getMessage().contains("Email 'existing@mams.mil' is already registered"));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("5. Admin attempting to deactivate or demote their own account is rejected (400)")
    void testAdminCannotDeactivateOrDemoteOwnAccount() {
        Long adminId = 1L;
        Authentication adminAuth = createAuth(adminId, "admin", Role.ADMIN, null);

        // Self-deactivation via deactivateUser endpoint
        BadRequestException deactEx = assertThrows(BadRequestException.class, () ->
                userService.deactivateUser(adminId, adminAuth));
        assertTrue(deactEx.getMessage().contains("Administrators cannot deactivate their own account"));

        // Self-deactivation via updateUser
        User adminUser = User.builder()
                .id(adminId)
                .username("admin")
                .email("admin@mams.mil")
                .fullName("System Admin")
                .role(Role.ADMIN)
                .isActive(true)
                .build();
        when(userRepository.findByIdWithBase(adminId)).thenReturn(Optional.of(adminUser));

        UpdateUserRequest deactUpdate = UpdateUserRequest.builder()
                .fullName("System Admin")
                .role(Role.ADMIN)
                .isActive(false) // Demoting active state
                .build();

        BadRequestException updateDeactEx = assertThrows(BadRequestException.class, () ->
                userService.updateUser(adminId, deactUpdate, adminAuth));
        assertTrue(updateDeactEx.getMessage().contains("Administrators cannot deactivate their own account"));

        // Self-demotion via updateUser
        UpdateUserRequest demoteUpdate = UpdateUserRequest.builder()
                .fullName("System Admin")
                .role(Role.BASE_COMMANDER) // Demoting role
                .baseId(1L)
                .isActive(true)
                .build();

        BadRequestException updateDemoteEx = assertThrows(BadRequestException.class, () ->
                userService.updateUser(adminId, demoteUpdate, adminAuth));
        assertTrue(updateDemoteEx.getMessage().contains("Administrators cannot demote their own account"));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("6. Password is never present in any returned DTO/JSON response")
    void testPasswordNeverPresentInReturnedDtoOrJson() throws Exception {
        Authentication adminAuth = createAuth(1L, "admin", Role.ADMIN, null);

        Base base = Base.builder().id(10L).name("Forward Operating Base Alpha").isActive(true).build();
        when(baseRepository.findById(10L)).thenReturn(Optional.of(base));
        when(userRepository.existsByUsername("new_commander")).thenReturn(false);
        when(userRepository.existsByEmail("new_cmdr@mams.mil")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashedPasswordValueWhichMustNeverLeak");

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(99L);
            return u;
        });

        CreateUserRequest createReq = CreateUserRequest.builder()
                .username("new_commander")
                .email("new_cmdr@mams.mil")
                .password("SuperSecretPass123!")
                .fullName("Major Sterling")
                .role(Role.BASE_COMMANDER)
                .baseId(10L)
                .build();

        UserDTO createdDto = userService.createUser(createReq, adminAuth);

        assertNotNull(createdDto);
        assertEquals(99L, createdDto.getId());
        assertEquals("new_commander", createdDto.getUsername());
        assertEquals("new_cmdr@mams.mil", createdDto.getEmail());
        assertEquals("Major Sterling", createdDto.getFullName());
        assertEquals(Role.BASE_COMMANDER, createdDto.getRole());
        assertEquals(10L, createdDto.getBaseId());
        assertEquals("Forward Operating Base Alpha", createdDto.getBaseName());
        assertTrue(createdDto.getIsActive());

        // Verify JSON serialization contains NO password or password_hash
        String json = objectMapper.writeValueAsString(createdDto);
        assertFalse(json.toLowerCase().contains("password"), "JSON must not contain password keyword");
        assertFalse(json.toLowerCase().contains("hash"), "JSON must not contain password hash");
        assertFalse(json.contains("SuperSecretPass123!"), "Raw password must not be present in JSON");
        assertFalse(json.contains("$2a$10$hashedPasswordValueWhichMustNeverLeak"), "Password hash must not be present in JSON");

        // Verify Get Users also doesn't expose passwords
        User user1 = User.builder()
                .id(1L)
                .username("admin")
                .email("admin@mams.mil")
                .passwordHash("$2a$10$hash1")
                .fullName("Admin")
                .role(Role.ADMIN)
                .isActive(true)
                .build();

        Page<User> userPage = new PageImpl<>(List.of(user1), PageRequest.of(0, 10), 1);
        when(userRepository.findUsersPaged(isNull(), isNull(), isNull(), any())).thenReturn(userPage);

        Page<UserDTO> dtoPage = userService.getUsers(null, null, null, PageRequest.of(0, 10), adminAuth);
        assertEquals(1, dtoPage.getContent().size());
        String pageJson = objectMapper.writeValueAsString(dtoPage.getContent());
        assertFalse(pageJson.toLowerCase().contains("password"));
        assertFalse(pageJson.toLowerCase().contains("hash"));
    }
}
