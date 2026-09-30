package com.mams.repository;

import com.mams.model.Transfer;
import com.mams.model.enums.TransferStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransferRepository extends JpaRepository<Transfer, Long>, JpaSpecificationExecutor<Transfer> {

    @Query("SELECT COALESCE(SUM(t.quantity), 0L) FROM Transfer t " +
           "WHERE t.status = :status " +
           "AND t.transferDate BETWEEN :start AND :end " +
           "AND (:baseId IS NULL OR t.toBase.id = :baseId) " +
           "AND (:equipmentTypeId IS NULL OR t.equipmentType.id = :equipmentTypeId)")
    Long sumTransfersInInRange(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end,
            @Param("baseId") Long baseId,
            @Param("equipmentTypeId") Long equipmentTypeId,
            @Param("status") TransferStatus status);

    @Query("SELECT COALESCE(SUM(t.quantity), 0L) FROM Transfer t " +
           "WHERE t.status = :status " +
           "AND t.transferDate < :date " +
           "AND (:baseId IS NULL OR t.toBase.id = :baseId) " +
           "AND (:equipmentTypeId IS NULL OR t.equipmentType.id = :equipmentTypeId)")
    Long sumTransfersInBeforeDate(
            @Param("date") LocalDate date,
            @Param("baseId") Long baseId,
            @Param("equipmentTypeId") Long equipmentTypeId,
            @Param("status") TransferStatus status);

    @Query("SELECT COALESCE(SUM(t.quantity), 0L) FROM Transfer t " +
           "WHERE t.status = :status " +
           "AND t.transferDate BETWEEN :start AND :end " +
           "AND (:baseId IS NULL OR t.fromBase.id = :baseId) " +
           "AND (:equipmentTypeId IS NULL OR t.equipmentType.id = :equipmentTypeId)")
    Long sumTransfersOutInRange(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end,
            @Param("baseId") Long baseId,
            @Param("equipmentTypeId") Long equipmentTypeId,
            @Param("status") TransferStatus status);

    @Query("SELECT COALESCE(SUM(t.quantity), 0L) FROM Transfer t " +
           "WHERE t.status = :status " +
           "AND t.transferDate < :date " +
           "AND (:baseId IS NULL OR t.fromBase.id = :baseId) " +
           "AND (:equipmentTypeId IS NULL OR t.equipmentType.id = :equipmentTypeId)")
    Long sumTransfersOutBeforeDate(
            @Param("date") LocalDate date,
            @Param("baseId") Long baseId,
            @Param("equipmentTypeId") Long equipmentTypeId,
            @Param("status") TransferStatus status);

    @Query("SELECT t FROM Transfer t " +
           "JOIN FETCH t.equipmentType " +
           "JOIN FETCH t.fromBase " +
           "JOIN FETCH t.toBase " +
           "WHERE t.status = :status " +
           "AND t.transferDate BETWEEN :start AND :end " +
           "AND (:baseId IS NULL OR t.toBase.id = :baseId) " +
           "AND (:equipmentTypeId IS NULL OR t.equipmentType.id = :equipmentTypeId) " +
           "ORDER BY t.transferDate DESC")
    List<Transfer> findTransfersInDetail(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end,
            @Param("baseId") Long baseId,
            @Param("equipmentTypeId") Long equipmentTypeId,
            @Param("status") TransferStatus status);

    @Query("SELECT t FROM Transfer t " +
           "JOIN FETCH t.equipmentType " +
           "JOIN FETCH t.fromBase " +
           "JOIN FETCH t.toBase " +
           "WHERE t.status = :status " +
           "AND t.transferDate BETWEEN :start AND :end " +
           "AND (:baseId IS NULL OR t.fromBase.id = :baseId) " +
           "AND (:equipmentTypeId IS NULL OR t.equipmentType.id = :equipmentTypeId) " +
           "ORDER BY t.transferDate DESC")
    List<Transfer> findTransfersOutDetail(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end,
            @Param("baseId") Long baseId,
            @Param("equipmentTypeId") Long equipmentTypeId,
            @Param("status") TransferStatus status);

    @Query("SELECT t FROM Transfer t WHERE (:baseId IS NULL OR t.fromBase.id = :baseId OR t.toBase.id = :baseId) " +
           "AND (:equipmentTypeId IS NULL OR t.equipmentType.id = :equipmentTypeId) " +
           "AND (:startDate IS NULL OR t.transferDate >= :startDate) " +
           "AND (:endDate IS NULL OR t.transferDate <= :endDate)")
    List<Transfer> findFiltered(
            @Param("baseId") Long baseId,
            @Param("equipmentTypeId") Long equipmentTypeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT t FROM Transfer t WHERE (:baseId IS NULL OR t.fromBase.id = :baseId OR t.toBase.id = :baseId) " +
           "AND (:equipmentTypeId IS NULL OR t.equipmentType.id = :equipmentTypeId) " +
           "AND (:startDate IS NULL OR t.transferDate >= :startDate) " +
           "AND (:endDate IS NULL OR t.transferDate <= :endDate)")
    Page<Transfer> findFilteredPaged(
            @Param("baseId") Long baseId,
            @Param("equipmentTypeId") Long equipmentTypeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable);

    @Query(value = "SELECT t FROM Transfer t " +
                   "JOIN FETCH t.equipmentType " +
                   "JOIN FETCH t.fromBase " +
                   "JOIN FETCH t.toBase " +
                   "JOIN FETCH t.createdBy " +
                   "WHERE (:baseId IS NULL OR " +
                   "  (:direction = 'in' AND t.toBase.id = :baseId) OR " +
                   "  (:direction = 'out' AND t.fromBase.id = :baseId) OR " +
                   "  ((:direction IS NULL OR (:direction != 'in' AND :direction != 'out')) AND (t.fromBase.id = :baseId OR t.toBase.id = :baseId))) " +
                   "AND (:equipmentTypeId IS NULL OR t.equipmentType.id = :equipmentTypeId) " +
                   "AND (:startDate IS NULL OR t.transferDate >= :startDate) " +
                   "AND (:endDate IS NULL OR t.transferDate <= :endDate)",
           countQuery = "SELECT COUNT(t) FROM Transfer t " +
                        "WHERE (:baseId IS NULL OR " +
                        "  (:direction = 'in' AND t.toBase.id = :baseId) OR " +
                        "  (:direction = 'out' AND t.fromBase.id = :baseId) OR " +
                        "  ((:direction IS NULL OR (:direction != 'in' AND :direction != 'out')) AND (t.fromBase.id = :baseId OR t.toBase.id = :baseId))) " +
                        "AND (:equipmentTypeId IS NULL OR t.equipmentType.id = :equipmentTypeId) " +
                        "AND (:startDate IS NULL OR t.transferDate >= :startDate) " +
                        "AND (:endDate IS NULL OR t.transferDate <= :endDate)")
    Page<Transfer> findTransfersPaged(
            @Param("baseId") Long baseId,
            @Param("direction") String direction,
            @Param("equipmentTypeId") Long equipmentTypeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable);

    @Query("SELECT t FROM Transfer t JOIN FETCH t.equipmentType JOIN FETCH t.fromBase JOIN FETCH t.toBase JOIN FETCH t.createdBy WHERE t.id = :id")
    java.util.Optional<Transfer> findByIdWithDetails(@Param("id") Long id);
}
