package com.mams.service;

import com.mams.dto.dashboard.DashboardMetricsDTO;
import com.mams.model.enums.AssignmentStatus;
import com.mams.model.enums.Role;
import com.mams.model.enums.TransferStatus;
import com.mams.repository.AssignmentExpenditureRepository;
import com.mams.repository.PurchaseRepository;
import com.mams.repository.TransferRepository;
import com.mams.security.BaseScopeResolver;
import com.mams.security.UserPrincipal;
import com.mams.service.impl.DashboardServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private PurchaseRepository purchaseRepository;

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private AssignmentExpenditureRepository assignmentExpenditureRepository;

    private BaseScopeResolver baseScopeResolver;
    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        baseScopeResolver = new BaseScopeResolver();
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
    @DisplayName("1. Admin with no base filter sums across all bases (baseId = null)")
    void testAdminWithNoBaseFilterSumsAcrossAllBases() {
        Authentication adminAuth = createAuth(1L, "admin", Role.ADMIN, null);
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);

        when(purchaseRepository.sumPurchasesInRange(eq(start), eq(end), isNull(), isNull())).thenReturn(500L);
        when(transferRepository.sumTransfersInInRange(eq(start), eq(end), isNull(), isNull(), eq(TransferStatus.completed))).thenReturn(200L);
        when(transferRepository.sumTransfersOutInRange(eq(start), eq(end), isNull(), isNull(), eq(TransferStatus.completed))).thenReturn(200L);
        when(assignmentExpenditureRepository.sumExpendedInRange(eq(start), eq(end), isNull(), isNull(), eq(AssignmentStatus.expended))).thenReturn(50L);

        when(purchaseRepository.sumPurchasesBeforeDate(eq(start), isNull(), isNull())).thenReturn(1000L);
        when(transferRepository.sumTransfersInBeforeDate(eq(start), isNull(), isNull(), eq(TransferStatus.completed))).thenReturn(100L);
        when(transferRepository.sumTransfersOutBeforeDate(eq(start), isNull(), isNull(), eq(TransferStatus.completed))).thenReturn(100L);
        when(assignmentExpenditureRepository.sumExpendedBeforeDate(eq(start), isNull(), isNull(), eq(AssignmentStatus.expended))).thenReturn(200L);

        when(assignmentExpenditureRepository.sumCurrentlyAssigned(isNull(), isNull(), eq(AssignmentStatus.assigned))).thenReturn(80L);

        DashboardMetricsDTO result = dashboardService.getMetrics(start, end, null, null, adminAuth);

        // Verify all repository calls passed null for baseId
        verify(purchaseRepository).sumPurchasesInRange(start, end, null, null);
        verify(transferRepository).sumTransfersInInRange(start, end, null, null, TransferStatus.completed);
        verify(transferRepository).sumTransfersOutInRange(start, end, null, null, TransferStatus.completed);
        verify(assignmentExpenditureRepository).sumExpendedInRange(start, end, null, null, AssignmentStatus.expended);

        // Opening = 1000 + 100 - 100 - 200 = 800
        assertEquals(800L, result.getOpeningBalance());
        // Net movement = 500 + 200 - 200 = 500
        assertEquals(500L, result.getNetMovement());
        // Closing = 800 + 500 - 50 = 1250
        assertEquals(1250L, result.getClosingBalance());
        assertEquals(80L, result.getAssigned());
    }

    @Test
    @DisplayName("2. Base Commander's requested base_id is ignored/overridden by their own base_id from token")
    void testBaseCommanderRequestedBaseIdOverriddenByTokenBaseId() {
        Long commanderBaseId = 2L;
        Long attemptedForeignBaseId = 99L;
        Authentication commanderAuth = createAuth(2L, "commander", Role.BASE_COMMANDER, commanderBaseId);

        LocalDate start = LocalDate.of(2026, 2, 1);
        LocalDate end = LocalDate.of(2026, 2, 28);

        when(purchaseRepository.sumPurchasesInRange(eq(start), eq(end), eq(commanderBaseId), isNull())).thenReturn(10L);
        when(transferRepository.sumTransfersInInRange(eq(start), eq(end), eq(commanderBaseId), isNull(), eq(TransferStatus.completed))).thenReturn(5L);
        when(transferRepository.sumTransfersOutInRange(eq(start), eq(end), eq(commanderBaseId), isNull(), eq(TransferStatus.completed))).thenReturn(2L);
        when(assignmentExpenditureRepository.sumExpendedInRange(eq(start), eq(end), eq(commanderBaseId), isNull(), eq(AssignmentStatus.expended))).thenReturn(1L);

        when(purchaseRepository.sumPurchasesBeforeDate(eq(start), eq(commanderBaseId), isNull())).thenReturn(20L);
        when(transferRepository.sumTransfersInBeforeDate(eq(start), eq(commanderBaseId), isNull(), eq(TransferStatus.completed))).thenReturn(0L);
        when(transferRepository.sumTransfersOutBeforeDate(eq(start), eq(commanderBaseId), isNull(), eq(TransferStatus.completed))).thenReturn(0L);
        when(assignmentExpenditureRepository.sumExpendedBeforeDate(eq(start), eq(commanderBaseId), isNull(), eq(AssignmentStatus.expended))).thenReturn(0L);

        when(assignmentExpenditureRepository.sumCurrentlyAssigned(eq(commanderBaseId), isNull(), eq(AssignmentStatus.assigned))).thenReturn(4L);

        DashboardMetricsDTO result = dashboardService.getMetrics(start, end, attemptedForeignBaseId, null, commanderAuth);

        // Verify that the attempted base ID (99L) was strictly IGNORED and commanderBaseId (2L) was used everywhere
        verify(purchaseRepository).sumPurchasesInRange(start, end, commanderBaseId, null);
        verify(purchaseRepository, never()).sumPurchasesInRange(start, end, attemptedForeignBaseId, null);
        verify(transferRepository).sumTransfersInInRange(start, end, commanderBaseId, null, TransferStatus.completed);
        verify(assignmentExpenditureRepository).sumCurrentlyAssigned(commanderBaseId, null, AssignmentStatus.assigned);

        assertEquals(commanderBaseId, result.getFilters().getBaseId());
    }

    @Test
    @DisplayName("3. opening_balance correctly reflects only pre-start_date activity")
    void testOpeningBalanceReflectsOnlyPreStartDateActivity() {
        Authentication adminAuth = createAuth(1L, "admin", Role.ADMIN, null);
        LocalDate start = LocalDate.of(2026, 3, 1);
        LocalDate end = LocalDate.of(2026, 3, 31);
        Long baseId = 1L;

        // Pre-start date activity (date < start):
        // Purchases = 60, Transfers In = 25, Transfers Out = 15, Expended = 10
        // Expected opening = 60 + 25 - 15 - 10 = 60
        when(purchaseRepository.sumPurchasesBeforeDate(eq(start), eq(baseId), isNull())).thenReturn(60L);
        when(transferRepository.sumTransfersInBeforeDate(eq(start), eq(baseId), isNull(), eq(TransferStatus.completed))).thenReturn(25L);
        when(transferRepository.sumTransfersOutBeforeDate(eq(start), eq(baseId), isNull(), eq(TransferStatus.completed))).thenReturn(15L);
        when(assignmentExpenditureRepository.sumExpendedBeforeDate(eq(start), eq(baseId), isNull(), eq(AssignmentStatus.expended))).thenReturn(10L);

        // In-range activity (completely different numbers):
        when(purchaseRepository.sumPurchasesInRange(eq(start), eq(end), eq(baseId), isNull())).thenReturn(999L);
        when(transferRepository.sumTransfersInInRange(eq(start), eq(end), eq(baseId), isNull(), eq(TransferStatus.completed))).thenReturn(888L);
        when(transferRepository.sumTransfersOutInRange(eq(start), eq(end), eq(baseId), isNull(), eq(TransferStatus.completed))).thenReturn(777L);
        when(assignmentExpenditureRepository.sumExpendedInRange(eq(start), eq(end), eq(baseId), isNull(), eq(AssignmentStatus.expended))).thenReturn(666L);
        when(assignmentExpenditureRepository.sumCurrentlyAssigned(eq(baseId), isNull(), eq(AssignmentStatus.assigned))).thenReturn(0L);

        DashboardMetricsDTO result = dashboardService.getMetrics(start, end, baseId, null, adminAuth);

        // Opening balance must be unaffected by in-range numbers
        assertEquals(60L, result.getOpeningBalance());
        verify(purchaseRepository).sumPurchasesBeforeDate(start, baseId, null);
        verify(transferRepository).sumTransfersInBeforeDate(start, baseId, null, TransferStatus.completed);
        verify(transferRepository).sumTransfersOutBeforeDate(start, baseId, null, TransferStatus.completed);
        verify(assignmentExpenditureRepository).sumExpendedBeforeDate(start, baseId, null, AssignmentStatus.expended);
    }

    @Test
    @DisplayName("4. closing_balance = opening_balance + net_movement - expended verified with hand-computed fixture")
    void testClosingBalanceCalculationWithHandComputedFixture() {
        Authentication auth = createAuth(1L, "admin", Role.ADMIN, null);
        LocalDate start = LocalDate.of(2026, 2, 1);
        LocalDate end = LocalDate.of(2026, 2, 28);
        Long baseId = 1L;

        // Fixture:
        // Pre-start:
        // Purchase 1 = 100
        // Transfer In 1 = 30
        // Transfer Out 1 = 10
        // Expenditure 1 = 20
        // Opening balance = 100 + 30 - 10 - 20 = 100
        when(purchaseRepository.sumPurchasesBeforeDate(eq(start), eq(baseId), isNull())).thenReturn(100L);
        when(transferRepository.sumTransfersInBeforeDate(eq(start), eq(baseId), isNull(), eq(TransferStatus.completed))).thenReturn(30L);
        when(transferRepository.sumTransfersOutBeforeDate(eq(start), eq(baseId), isNull(), eq(TransferStatus.completed))).thenReturn(10L);
        when(assignmentExpenditureRepository.sumExpendedBeforeDate(eq(start), eq(baseId), isNull(), eq(AssignmentStatus.expended))).thenReturn(20L);

        // In-range [Feb 1 to Feb 28]:
        // Purchases (e.g. Purchase 2 = 50, Purchase 3 = 25) -> Sum = 75
        // Transfer In 2 = 40
        // Transfer Out 2 = 15
        // Net Movement = 75 + 40 - 15 = 100
        // Expended in-range = 30
        // Expected Closing Balance = 100 (opening) + 100 (net) - 30 (expended) = 170
        // Currently Assigned snapshot = 15
        when(purchaseRepository.sumPurchasesInRange(eq(start), eq(end), eq(baseId), isNull())).thenReturn(75L);
        when(transferRepository.sumTransfersInInRange(eq(start), eq(end), eq(baseId), isNull(), eq(TransferStatus.completed))).thenReturn(40L);
        when(transferRepository.sumTransfersOutInRange(eq(start), eq(end), eq(baseId), isNull(), eq(TransferStatus.completed))).thenReturn(15L);
        when(assignmentExpenditureRepository.sumExpendedInRange(eq(start), eq(end), eq(baseId), isNull(), eq(AssignmentStatus.expended))).thenReturn(30L);

        when(assignmentExpenditureRepository.sumCurrentlyAssigned(eq(baseId), isNull(), eq(AssignmentStatus.assigned))).thenReturn(15L);

        DashboardMetricsDTO result = dashboardService.getMetrics(start, end, baseId, null, auth);

        assertEquals(100L, result.getOpeningBalance(), "Opening balance must match formula");
        assertEquals(75L, result.getPurchases(), "In-range purchases must be 75");
        assertEquals(40L, result.getTransfersIn(), "In-range transfers in must be 40");
        assertEquals(15L, result.getTransfersOut(), "In-range transfers out must be 15");
        assertEquals(100L, result.getNetMovement(), "Net movement must equal purchases + transfersIn - transfersOut = 100");
        assertEquals(30L, result.getExpended(), "Expended must be 30");
        assertEquals(170L, result.getClosingBalance(), "Closing balance must equal opening + net_movement - expended = 170");
        assertEquals(15L, result.getAssigned(), "Assigned snapshot must be 15");
    }
}
