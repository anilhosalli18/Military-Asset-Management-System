package com.mams.service;

import com.mams.dto.purchase.CreatePurchaseRequest;
import com.mams.dto.purchase.PurchaseDTO;
import com.mams.exception.BadRequestException;
import com.mams.model.Base;
import com.mams.model.EquipmentType;
import com.mams.model.Purchase;
import com.mams.model.User;
import com.mams.model.enums.EquipmentCategory;
import com.mams.model.enums.Role;
import com.mams.repository.BaseRepository;
import com.mams.repository.EquipmentTypeRepository;
import com.mams.repository.PurchaseRepository;
import com.mams.repository.UserRepository;
import com.mams.security.BaseScopeResolver;
import com.mams.security.UserPrincipal;
import com.mams.service.impl.PurchaseServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurchaseServiceTest {

    @Mock
    private PurchaseRepository purchaseRepository;

    @Mock
    private BaseRepository baseRepository;

    @Mock
    private EquipmentTypeRepository equipmentTypeRepository;

    @Mock
    private UserRepository userRepository;

    private BaseScopeResolver baseScopeResolver;
    private PurchaseService purchaseService;

    @BeforeEach
    void setUp() {
        baseScopeResolver = new BaseScopeResolver();
        purchaseService = new PurchaseServiceImpl(
                purchaseRepository,
                baseRepository,
                equipmentTypeRepository,
                userRepository,
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
    @DisplayName("1. ADMIN can create a purchase for any base")
    void testAdminCanCreatePurchaseForAnyBase() {
        Authentication adminAuth = createAuth(1L, "admin", Role.ADMIN, null);
        Long targetBaseId = 2L;

        Base base = Base.builder().id(targetBaseId).name("Forward Operating Base Alpha").isActive(true).build();
        EquipmentType eq = EquipmentType.builder().id(10L).name("M4A1 Carbine").category(EquipmentCategory.weapon).unit("pcs").build();
        User adminUser = User.builder().id(1L).username("admin").build();

        CreatePurchaseRequest request = CreatePurchaseRequest.builder()
                .baseId(targetBaseId)
                .equipmentTypeId(10L)
                .quantity(50)
                .unitCost(new BigDecimal("1200.00"))
                .vendor("Colt Defense")
                .purchaseDate(LocalDate.now().minusDays(1))
                .notes("New consignment")
                .build();

        when(baseRepository.findById(targetBaseId)).thenReturn(Optional.of(base));
        when(equipmentTypeRepository.findById(10L)).thenReturn(Optional.of(eq));
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(purchaseRepository.save(any(Purchase.class))).thenAnswer(invocation -> {
            Purchase p = invocation.getArgument(0);
            p.setId(101L);
            return p;
        });

        PurchaseDTO result = purchaseService.createPurchase(request, adminAuth);

        assertNotNull(result);
        assertEquals(101L, result.getId());
        assertEquals(targetBaseId, result.getBaseId());
        assertEquals("Forward Operating Base Alpha", result.getBaseName());
        assertEquals(10L, result.getEquipmentTypeId());
        assertEquals(50, result.getQuantity());
        assertEquals(new BigDecimal("60000.00"), result.getTotalCost());
        assertEquals("admin", result.getCreatedByUsername());

        ArgumentCaptor<Purchase> captor = ArgumentCaptor.forClass(Purchase.class);
        verify(purchaseRepository).save(captor.capture());
        assertEquals(targetBaseId, captor.getValue().getBase().getId());
    }

    @Test
    @DisplayName("2. BASE_COMMANDER's request is forced to their own base even if they pass a different base_id")
    void testBaseCommanderRequestForcedToOwnBaseEvenIfDifferentBaseIdPassed() {
        Long commanderBaseId = 1L;
        Long attemptedOtherBaseId = 99L;
        Authentication commanderAuth = createAuth(2L, "commander_john", Role.BASE_COMMANDER, commanderBaseId);

        Base assignedBase = Base.builder().id(commanderBaseId).name("Headquarters Base").isActive(true).build();
        EquipmentType eq = EquipmentType.builder().id(5L).name("JLTV Armor Vehicle").category(EquipmentCategory.vehicle).unit("units").build();
        User commanderUser = User.builder().id(2L).username("commander_john").build();

        // The request specifies attemptedOtherBaseId (99L)
        CreatePurchaseRequest request = CreatePurchaseRequest.builder()
                .baseId(attemptedOtherBaseId)
                .equipmentTypeId(5L)
                .quantity(10)
                .unitCost(new BigDecimal("250000.00"))
                .vendor("Oshkosh Defense")
                .purchaseDate(LocalDate.now())
                .build();

        when(baseRepository.findById(commanderBaseId)).thenReturn(Optional.of(assignedBase));
        when(equipmentTypeRepository.findById(5L)).thenReturn(Optional.of(eq));
        when(userRepository.findById(2L)).thenReturn(Optional.of(commanderUser));
        when(purchaseRepository.save(any(Purchase.class))).thenAnswer(invocation -> {
            Purchase p = invocation.getArgument(0);
            p.setId(202L);
            return p;
        });

        PurchaseDTO result = purchaseService.createPurchase(request, commanderAuth);

        assertNotNull(result);
        assertEquals(202L, result.getId());
        // Must be forced to commander's assigned base (1L), NOT 99L!
        assertEquals(commanderBaseId, result.getBaseId());
        assertEquals("Headquarters Base", result.getBaseName());

        ArgumentCaptor<Purchase> captor = ArgumentCaptor.forClass(Purchase.class);
        verify(purchaseRepository).save(captor.capture());
        assertEquals(commanderBaseId, captor.getValue().getBase().getId());
        // Verify base 99 was never queried or assigned
        verify(baseRepository, never()).findById(attemptedOtherBaseId);
    }

    @Test
    @DisplayName("3. Creating a purchase with quantity = 0 or negative is rejected")
    void testCreatingPurchaseWithZeroOrNegativeQuantityIsRejected() {
        Authentication adminAuth = createAuth(1L, "admin", Role.ADMIN, null);

        // Quantity = 0
        CreatePurchaseRequest zeroQtyRequest = CreatePurchaseRequest.builder()
                .baseId(1L)
                .equipmentTypeId(10L)
                .quantity(0)
                .purchaseDate(LocalDate.now())
                .build();

        BadRequestException exZero = assertThrows(BadRequestException.class, () ->
                purchaseService.createPurchase(zeroQtyRequest, adminAuth));
        assertTrue(exZero.getMessage().contains("Quantity must be greater than zero"));

        // Quantity = -5
        CreatePurchaseRequest negativeQtyRequest = CreatePurchaseRequest.builder()
                .baseId(1L)
                .equipmentTypeId(10L)
                .quantity(-5)
                .purchaseDate(LocalDate.now())
                .build();

        BadRequestException exNegative = assertThrows(BadRequestException.class, () ->
                purchaseService.createPurchase(negativeQtyRequest, adminAuth));
        assertTrue(exNegative.getMessage().contains("Quantity must be greater than zero"));

        // Null quantity
        CreatePurchaseRequest nullQtyRequest = CreatePurchaseRequest.builder()
                .baseId(1L)
                .equipmentTypeId(10L)
                .quantity(null)
                .purchaseDate(LocalDate.now())
                .build();

        assertThrows(BadRequestException.class, () ->
                purchaseService.createPurchase(nullQtyRequest, adminAuth));

        verify(purchaseRepository, never()).save(any(Purchase.class));
    }

    @Test
    @DisplayName("4. getPurchases filters correctly by date range and equipment_type_id")
    void testGetPurchasesFiltersCorrectlyByDateRangeAndEquipmentTypeId() {
        Authentication adminAuth = createAuth(1L, "admin", Role.ADMIN, null);
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);
        Long equipmentTypeId = 15L;
        Pageable pageable = PageRequest.of(0, 10);

        Base base = Base.builder().id(1L).name("HQ Base").build();
        EquipmentType eq = EquipmentType.builder().id(equipmentTypeId).name("5.56mm Ammo").build();
        User user = User.builder().id(1L).username("admin").build();

        Purchase purchase1 = Purchase.builder()
                .id(1L)
                .base(base)
                .equipmentType(eq)
                .quantity(1000)
                .unitCost(new BigDecimal("0.50"))
                .totalCost(new BigDecimal("500.00"))
                .purchaseDate(LocalDate.of(2026, 1, 15))
                .createdBy(user)
                .build();

        Page<Purchase> mockPage = new PageImpl<>(List.of(purchase1), pageable, 1);
        when(purchaseRepository.findFilteredPaged(isNull(), eq(equipmentTypeId), eq(start), eq(end), any(Pageable.class)))
                .thenReturn(mockPage);

        Page<PurchaseDTO> result = purchaseService.getPurchases(start, end, null, equipmentTypeId, pageable, adminAuth);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        PurchaseDTO dto = result.getContent().get(0);
        assertEquals(1L, dto.getId());
        assertEquals(equipmentTypeId, dto.getEquipmentTypeId());
        assertEquals("5.56mm Ammo", dto.getEquipmentName());
        assertEquals(1000, dto.getQuantity());

        verify(purchaseRepository).findFilteredPaged(isNull(), eq(equipmentTypeId), eq(start), eq(end), any(Pageable.class));
    }

    @Test
    @DisplayName("5. A BASE_COMMANDER's getPurchases call never returns rows from another base")
    void testBaseCommanderGetPurchasesNeverReturnsRowsFromAnotherBase() {
        Long commanderBaseId = 1L;
        Long attemptedOtherBaseId = 2L;
        Authentication commanderAuth = createAuth(5L, "base_commander", Role.BASE_COMMANDER, commanderBaseId);

        Pageable pageable = PageRequest.of(0, 10);
        when(purchaseRepository.findFilteredPaged(eq(commanderBaseId), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList(), pageable, 0));

        // Commander attempts to query base 2L
        Page<PurchaseDTO> result = purchaseService.getPurchases(null, null, attemptedOtherBaseId, null, pageable, commanderAuth);

        assertNotNull(result);
        // Verify that the repository was called with commanderBaseId (1L), NOT attemptedOtherBaseId (2L)
        verify(purchaseRepository).findFilteredPaged(eq(commanderBaseId), isNull(), isNull(), isNull(), any(Pageable.class));
        verify(purchaseRepository, never()).findFilteredPaged(eq(attemptedOtherBaseId), any(), any(), any(), any());
    }
}
