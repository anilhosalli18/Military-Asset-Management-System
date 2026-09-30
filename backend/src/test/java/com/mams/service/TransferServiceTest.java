package com.mams.service;

import com.mams.dto.transfer.CreateTransferRequest;
import com.mams.dto.transfer.TransferDTO;
import com.mams.exception.BadRequestException;
import com.mams.exception.UnauthorizedBaseAccessException;
import com.mams.model.Base;
import com.mams.model.EquipmentType;
import com.mams.model.Transfer;
import com.mams.model.User;
import com.mams.model.enums.EquipmentCategory;
import com.mams.model.enums.Role;
import com.mams.model.enums.TransferStatus;
import com.mams.repository.BaseRepository;
import com.mams.repository.EquipmentTypeRepository;
import com.mams.repository.TransferRepository;
import com.mams.repository.UserRepository;
import com.mams.security.UserPrincipal;
import com.mams.service.impl.TransferServiceImpl;
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

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private BaseRepository baseRepository;

    @Mock
    private EquipmentTypeRepository equipmentTypeRepository;

    @Mock
    private UserRepository userRepository;

    private TransferService transferService;

    @BeforeEach
    void setUp() {
        transferService = new TransferServiceImpl(
                transferRepository,
                baseRepository,
                equipmentTypeRepository,
                userRepository
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
    @DisplayName("1. from_base_id == to_base_id is rejected with 400")
    void testFromBaseEqualsToBaseIsRejected() {
        Authentication adminAuth = createAuth(1L, "admin", Role.ADMIN, null);

        CreateTransferRequest request = CreateTransferRequest.builder()
                .fromBaseId(1L)
                .toBaseId(1L)
                .equipmentTypeId(10L)
                .quantity(15)
                .transferDate(LocalDate.now())
                .build();

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                transferService.createTransfer(request, adminAuth));

        assertTrue(ex.getMessage().contains("bases cannot be identical"));
        verify(transferRepository, never()).save(any(Transfer.class));
    }

    @Test
    @DisplayName("2. BASE_COMMANDER can create a transfer FROM their base TO another base")
    void testBaseCommanderCanCreateTransferFromOwnBaseToAnotherBase() {
        Long commanderBaseId = 1L;
        Long destinationBaseId = 2L;
        Authentication commanderAuth = createAuth(2L, "commander_john", Role.BASE_COMMANDER, commanderBaseId);

        Base fromBase = Base.builder().id(commanderBaseId).name("HQ Base").isActive(true).build();
        Base toBase = Base.builder().id(destinationBaseId).name("Outpost Alpha").isActive(true).build();
        EquipmentType eq = EquipmentType.builder().id(5L).name("Humvee").category(EquipmentCategory.vehicle).unit("units").build();
        User commander = User.builder().id(2L).username("commander_john").build();

        CreateTransferRequest request = CreateTransferRequest.builder()
                .fromBaseId(commanderBaseId)
                .toBaseId(destinationBaseId)
                .equipmentTypeId(5L)
                .quantity(3)
                .transferDate(LocalDate.now())
                .notes("Reallocation")
                .build();

        when(baseRepository.findById(commanderBaseId)).thenReturn(Optional.of(fromBase));
        when(baseRepository.findById(destinationBaseId)).thenReturn(Optional.of(toBase));
        when(equipmentTypeRepository.findById(5L)).thenReturn(Optional.of(eq));
        when(userRepository.findById(2L)).thenReturn(Optional.of(commander));
        when(transferRepository.save(any(Transfer.class))).thenAnswer(invocation -> {
            Transfer t = invocation.getArgument(0);
            t.setId(501L);
            return t;
        });

        TransferDTO result = transferService.createTransfer(request, commanderAuth);

        assertNotNull(result);
        assertEquals(501L, result.getId());
        assertEquals(commanderBaseId, result.getFromBaseId());
        assertEquals("HQ Base", result.getFromBaseName());
        assertEquals(destinationBaseId, result.getToBaseId());
        assertEquals("Outpost Alpha", result.getToBaseName());
        assertEquals(3, result.getQuantity());
        // Verify default status is 'completed'
        assertEquals(TransferStatus.completed, result.getStatus());

        ArgumentCaptor<Transfer> captor = ArgumentCaptor.forClass(Transfer.class);
        verify(transferRepository).save(captor.capture());
        assertEquals(TransferStatus.completed, captor.getValue().getStatus());
        assertEquals(commanderBaseId, captor.getValue().getFromBase().getId());
        assertEquals(destinationBaseId, captor.getValue().getToBase().getId());
    }

    @Test
    @DisplayName("3. BASE_COMMANDER is rejected (403) attempting to create a transfer between two bases neither of which is their own")
    void testBaseCommanderRejectedAttemptingToTransferBetweenUnownedBases() {
        Long commanderBaseId = 1L;
        Long otherBaseA = 2L;
        Long otherBaseB = 3L;
        Authentication commanderAuth = createAuth(2L, "commander_john", Role.BASE_COMMANDER, commanderBaseId);

        CreateTransferRequest request = CreateTransferRequest.builder()
                .fromBaseId(otherBaseA)
                .toBaseId(otherBaseB)
                .equipmentTypeId(5L)
                .quantity(10)
                .transferDate(LocalDate.now())
                .build();

        UnauthorizedBaseAccessException ex = assertThrows(UnauthorizedBaseAccessException.class, () ->
                transferService.createTransfer(request, commanderAuth));

        assertTrue(ex.getMessage().contains("You may only initiate transfers where your base is either the sender or receiver"));
        verify(transferRepository, never()).save(any(Transfer.class));
    }

    @Test
    @DisplayName("4. getTransfers with direction='out' only returns transfers where the effective base is the sender")
    void testGetTransfersWithDirectionOutReturnsTransfersWhereEffectiveBaseIsSender() {
        Long commanderBaseId = 1L;
        Authentication commanderAuth = createAuth(5L, "commander_tom", Role.BASE_COMMANDER, commanderBaseId);

        Pageable pageable = PageRequest.of(0, 10);
        Base fromBase = Base.builder().id(commanderBaseId).name("Base 1").build();
        Base toBase = Base.builder().id(2L).name("Base 2").build();
        EquipmentType eq = EquipmentType.builder().id(10L).name("Rifle").build();
        User user = User.builder().id(5L).username("commander_tom").build();

        Transfer transferOut = Transfer.builder()
                .id(1L)
                .fromBase(fromBase)
                .toBase(toBase)
                .equipmentType(eq)
                .quantity(20)
                .transferDate(LocalDate.now())
                .status(TransferStatus.completed)
                .createdBy(user)
                .build();

        Page<Transfer> mockPage = new PageImpl<>(List.of(transferOut), pageable, 1);
        when(transferRepository.findTransfersPaged(eq(commanderBaseId), eq("out"), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(mockPage);

        Page<TransferDTO> result = transferService.getTransfers(null, null, 99L, null, "out", pageable, commanderAuth);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(commanderBaseId, result.getContent().get(0).getFromBaseId());

        // Verify repository was called strictly with commanderBaseId (1L) and direction="out"
        verify(transferRepository).findTransfersPaged(eq(commanderBaseId), eq("out"), isNull(), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    @DisplayName("5. Invalid status transition (completed -> pending) is rejected")
    void testInvalidStatusTransitionCompletedToPendingIsRejected() {
        Long transferId = 100L;
        Authentication adminAuth = createAuth(1L, "admin", Role.ADMIN, null);

        Base fromBase = Base.builder().id(1L).name("Base 1").build();
        Base toBase = Base.builder().id(2L).name("Base 2").build();
        EquipmentType eq = EquipmentType.builder().id(10L).name("Rifle").build();
        User creator = User.builder().id(1L).username("admin").build();

        Transfer completedTransfer = Transfer.builder()
                .id(transferId)
                .fromBase(fromBase)
                .toBase(toBase)
                .equipmentType(eq)
                .quantity(10)
                .transferDate(LocalDate.now().minusDays(2))
                .status(TransferStatus.completed)
                .createdBy(creator)
                .build();

        when(transferRepository.findByIdWithDetails(transferId)).thenReturn(Optional.of(completedTransfer));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                transferService.updateTransferStatus(transferId, TransferStatus.pending, adminAuth));

        assertTrue(ex.getMessage().contains("Invalid status transition from completed to pending"));
        verify(transferRepository, never()).save(any(Transfer.class));
    }
}
