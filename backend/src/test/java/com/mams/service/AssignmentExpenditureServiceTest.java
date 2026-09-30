package com.mams.service;

import com.mams.dto.assignment.AssignmentDTO;
import com.mams.dto.assignment.CreateAssignmentRequest;
import com.mams.dto.dashboard.DashboardMetricsDTO;
import com.mams.exception.BadRequestException;
import com.mams.exception.UnauthorizedBaseAccessException;
import com.mams.model.AssignmentExpenditure;
import com.mams.model.Base;
import com.mams.model.EquipmentType;
import com.mams.model.User;
import com.mams.model.enums.AssignmentStatus;
import com.mams.model.enums.EquipmentCategory;
import com.mams.model.enums.Role;
import com.mams.repository.*;
import com.mams.security.BaseScopeResolver;
import com.mams.security.UserPrincipal;
import com.mams.service.impl.AssignmentExpenditureServiceImpl;
import com.mams.service.impl.DashboardServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssignmentExpenditureServiceTest {

    @Mock
    private AssignmentExpenditureRepository assignmentExpenditureRepository;

    @Mock
    private BaseRepository baseRepository;

    @Mock
    private EquipmentTypeRepository equipmentTypeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PurchaseRepository purchaseRepository;

    @Mock
    private TransferRepository transferRepository;

    private BaseScopeResolver baseScopeResolver;
    private AssignmentExpenditureService assignmentService;
    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        baseScopeResolver = new BaseScopeResolver();
        assignmentService = new AssignmentExpenditureServiceImpl(
                assignmentExpenditureRepository,
                baseRepository,
                equipmentTypeRepository,
                userRepository,
                baseScopeResolver
        );

        dashboardService = new DashboardServiceImpl(
                purchaseRepository,
                transferRepository,
                assignmentExpenditureRepository,
                baseScopeResolver
        );
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
    @DisplayName("1. LOGISTICS_OFFICER is rejected (403) on create, list, expend, and return")
    void testLogisticsOfficerRejectedOnAllAssignmentOperations() {
        Authentication logisticsAuth = createAuth(3L, "logistics_user", Role.LOGISTICS_OFFICER, 1L);

        CreateAssignmentRequest createReq = CreateAssignmentRequest.builder()
                .baseId(1L)
                .equipmentTypeId(5L)
                .personnelName("Sgt. Miller")
                .quantity(10)
                .assignedDate(LocalDate.now())
                .build();

        // 1. Create rejected with 403
        assertThrows(UnauthorizedBaseAccessException.class, () ->
                assignmentService.createAssignment(createReq, logisticsAuth));

        // 2. List rejected with 403
        assertThrows(UnauthorizedBaseAccessException.class, () ->
                assignmentService.getAssignments(null, null, null, null, null, PageRequest.of(0, 10), logisticsAuth));

        // 3. Expend rejected with 403
        assertThrows(UnauthorizedBaseAccessException.class, () ->
                assignmentService.markExpended(1L, LocalDate.now(), "Notes", logisticsAuth));

        // 4. Return rejected with 403
        assertThrows(UnauthorizedBaseAccessException.class, () ->
                assignmentService.markReturned(1L, LocalDate.now(), "Notes", logisticsAuth));

        verify(assignmentExpenditureRepository, never()).save(any(AssignmentExpenditure.class));
    }

    @Test
    @DisplayName("2. BASE_COMMANDER's create request is forced to their own base")
    void testBaseCommanderCreateForcedToOwnBase() {
        Long commanderBaseId = 1L;
        Long attemptedOtherBaseId = 99L;
        Authentication commanderAuth = createAuth(2L, "commander_john", Role.BASE_COMMANDER, commanderBaseId);

        Base assignedBase = Base.builder().id(commanderBaseId).name("HQ Base").isActive(true).build();
        EquipmentType eq = EquipmentType.builder().id(10L).name("M4 Rifle").category(EquipmentCategory.weapon).unit("pcs").build();
        User commander = User.builder().id(2L).username("commander_john").build();

        CreateAssignmentRequest request = CreateAssignmentRequest.builder()
                .baseId(attemptedOtherBaseId)
                .equipmentTypeId(10L)
                .personnelName("Cpl. Adams")
                .personnelIdNo("MIL-99881")
                .quantity(5)
                .assignedDate(LocalDate.now())
                .notes("Deployed on recon")
                .build();

        when(baseRepository.findById(commanderBaseId)).thenReturn(Optional.of(assignedBase));
        when(equipmentTypeRepository.findById(10L)).thenReturn(Optional.of(eq));
        when(userRepository.findById(2L)).thenReturn(Optional.of(commander));
        when(assignmentExpenditureRepository.save(any(AssignmentExpenditure.class))).thenAnswer(invocation -> {
            AssignmentExpenditure a = invocation.getArgument(0);
            a.setId(401L);
            return a;
        });

        AssignmentDTO result = assignmentService.createAssignment(request, commanderAuth);

        assertNotNull(result);
        assertEquals(401L, result.getId());
        assertEquals(commanderBaseId, result.getBaseId());
        assertEquals("HQ Base", result.getBaseName());
        assertEquals(AssignmentStatus.assigned, result.getStatus());

        ArgumentCaptor<AssignmentExpenditure> captor = ArgumentCaptor.forClass(AssignmentExpenditure.class);
        verify(assignmentExpenditureRepository).save(captor.capture());
        assertEquals(commanderBaseId, captor.getValue().getBase().getId());
        verify(baseRepository, never()).findById(attemptedOtherBaseId);
    }

    @Test
    @DisplayName("3. markExpended fails (400) if the assignment is already expended or returned")
    void testMarkExpendedFailsIfAlreadyExpendedOrReturned() {
        Authentication adminAuth = createAuth(1L, "admin", Role.ADMIN, null);
        Base base = Base.builder().id(1L).name("HQ Base").build();

        // 1. Already Expended
        AssignmentExpenditure alreadyExpended = AssignmentExpenditure.builder()
                .id(10L)
                .base(base)
                .status(AssignmentStatus.expended)
                .expendedDate(LocalDate.now().minusDays(1))
                .quantity(5)
                .build();

        when(assignmentExpenditureRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(alreadyExpended));

        BadRequestException ex1 = assertThrows(BadRequestException.class, () ->
                assignmentService.markExpended(10L, LocalDate.now(), "Extra note", adminAuth));
        assertTrue(ex1.getMessage().contains("Cannot expend assignment with status: expended"));

        // 2. Already Returned
        AssignmentExpenditure alreadyReturned = AssignmentExpenditure.builder()
                .id(11L)
                .base(base)
                .status(AssignmentStatus.returned)
                .returnedDate(LocalDate.now().minusDays(1))
                .quantity(5)
                .build();

        when(assignmentExpenditureRepository.findByIdWithDetails(11L)).thenReturn(Optional.of(alreadyReturned));

        BadRequestException ex2 = assertThrows(BadRequestException.class, () ->
                assignmentService.markExpended(11L, LocalDate.now(), "Extra note", adminAuth));
        assertTrue(ex2.getMessage().contains("Cannot expend assignment with status: returned"));

        verify(assignmentExpenditureRepository, never()).save(any(AssignmentExpenditure.class));
    }

    @Test
    @DisplayName("4. markReturned fails (400) if the assignment is already expended or returned")
    void testMarkReturnedFailsIfAlreadyExpendedOrReturned() {
        Authentication adminAuth = createAuth(1L, "admin", Role.ADMIN, null);
        Base base = Base.builder().id(1L).name("HQ Base").build();

        // 1. Already Expended
        AssignmentExpenditure alreadyExpended = AssignmentExpenditure.builder()
                .id(20L)
                .base(base)
                .status(AssignmentStatus.expended)
                .expendedDate(LocalDate.now().minusDays(2))
                .quantity(3)
                .build();

        when(assignmentExpenditureRepository.findByIdWithDetails(20L)).thenReturn(Optional.of(alreadyExpended));

        BadRequestException ex1 = assertThrows(BadRequestException.class, () ->
                assignmentService.markReturned(20L, LocalDate.now(), "Return note", adminAuth));
        assertTrue(ex1.getMessage().contains("Cannot return assignment with status: expended"));

        // 2. Already Returned
        AssignmentExpenditure alreadyReturned = AssignmentExpenditure.builder()
                .id(21L)
                .base(base)
                .status(AssignmentStatus.returned)
                .returnedDate(LocalDate.now().minusDays(2))
                .quantity(3)
                .build();

        when(assignmentExpenditureRepository.findByIdWithDetails(21L)).thenReturn(Optional.of(alreadyReturned));

        BadRequestException ex2 = assertThrows(BadRequestException.class, () ->
                assignmentService.markReturned(21L, LocalDate.now(), "Return note", adminAuth));
        assertTrue(ex2.getMessage().contains("Cannot return assignment with status: returned"));

        verify(assignmentExpenditureRepository, never()).save(any(AssignmentExpenditure.class));
    }

    @Test
    @DisplayName("5. Dashboard's expended metric reflects a newly-expended assignment when queried for date range")
    void testDashboardExpendedMetricReflectsNewlyExpendedAssignment() {
        Authentication adminAuth = createAuth(1L, "admin", Role.ADMIN, null);
        LocalDate start = LocalDate.of(2026, 3, 1);
        LocalDate end = LocalDate.of(2026, 3, 31);
        LocalDate expendedDate = LocalDate.of(2026, 3, 15);
        Long baseId = 1L;

        Base base = Base.builder().id(baseId).name("HQ Base").build();
        EquipmentType eq = EquipmentType.builder().id(10L).name("5.56mm Ammo").build();

        AssignmentExpenditure assigned = AssignmentExpenditure.builder()
                .id(30L)
                .base(base)
                .equipmentType(eq)
                .quantity(250)
                .status(AssignmentStatus.assigned)
                .assignedDate(LocalDate.of(2026, 3, 10))
                .build();

        when(assignmentExpenditureRepository.findByIdWithDetails(30L)).thenReturn(Optional.of(assigned));
        when(assignmentExpenditureRepository.save(any(AssignmentExpenditure.class))).thenAnswer(i -> i.getArgument(0));

        // Mark as expended
        AssignmentDTO updated = assignmentService.markExpended(30L, expendedDate, "Spent in firing drill", adminAuth);
        assertEquals(AssignmentStatus.expended, updated.getStatus());
        assertEquals(expendedDate, updated.getExpendedDate());

        // Verify DashboardService picks up the newly expended row in repository sumExpendedInRange
        when(assignmentExpenditureRepository.sumExpendedInRange(eq(start), eq(end), eq(baseId), isNull(), eq(AssignmentStatus.expended)))
                .thenReturn(250L);

        DashboardMetricsDTO metrics = dashboardService.getMetrics(start, end, baseId, null, adminAuth);

        assertNotNull(metrics);
        assertEquals(250L, metrics.getExpended());
        verify(assignmentExpenditureRepository).sumExpendedInRange(eq(start), eq(end), eq(baseId), isNull(), eq(AssignmentStatus.expended));
    }
}
