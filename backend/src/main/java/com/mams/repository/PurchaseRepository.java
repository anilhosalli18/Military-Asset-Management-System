package com.mams.repository;

import com.mams.model.Purchase;
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
public interface PurchaseRepository extends JpaRepository<Purchase, Long>, JpaSpecificationExecutor<Purchase> {

    List<Purchase> findByBaseId(Long baseId);

    @Query("SELECT COALESCE(SUM(p.quantity), 0L) FROM Purchase p " +
           "WHERE p.purchaseDate BETWEEN :start AND :end " +
           "AND (:baseId IS NULL OR p.base.id = :baseId) " +
           "AND (:equipmentTypeId IS NULL OR p.equipmentType.id = :equipmentTypeId)")
    Long sumPurchasesInRange(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end,
            @Param("baseId") Long baseId,
            @Param("equipmentTypeId") Long equipmentTypeId);

    @Query("SELECT COALESCE(SUM(p.quantity), 0L) FROM Purchase p " +
           "WHERE p.purchaseDate < :date " +
           "AND (:baseId IS NULL OR p.base.id = :baseId) " +
           "AND (:equipmentTypeId IS NULL OR p.equipmentType.id = :equipmentTypeId)")
    Long sumPurchasesBeforeDate(
            @Param("date") LocalDate date,
            @Param("baseId") Long baseId,
            @Param("equipmentTypeId") Long equipmentTypeId);

    @Query("SELECT p FROM Purchase p " +
           "JOIN FETCH p.base " +
           "JOIN FETCH p.equipmentType " +
           "WHERE p.purchaseDate BETWEEN :start AND :end " +
           "AND (:baseId IS NULL OR p.base.id = :baseId) " +
           "AND (:equipmentTypeId IS NULL OR p.equipmentType.id = :equipmentTypeId) " +
           "ORDER BY p.purchaseDate DESC")
    List<Purchase> findPurchasesDetail(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end,
            @Param("baseId") Long baseId,
            @Param("equipmentTypeId") Long equipmentTypeId);

    @Query("SELECT p FROM Purchase p WHERE (:baseId IS NULL OR p.base.id = :baseId) " +
           "AND (:equipmentTypeId IS NULL OR p.equipmentType.id = :equipmentTypeId) " +
           "AND (:startDate IS NULL OR p.purchaseDate >= :startDate) " +
           "AND (:endDate IS NULL OR p.purchaseDate <= :endDate)")
    List<Purchase> findFiltered(
            @Param("baseId") Long baseId,
            @Param("equipmentTypeId") Long equipmentTypeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query(value = "SELECT p FROM Purchase p JOIN FETCH p.base JOIN FETCH p.equipmentType JOIN FETCH p.createdBy " +
                   "WHERE (:baseId IS NULL OR p.base.id = :baseId) " +
                   "AND (:equipmentTypeId IS NULL OR p.equipmentType.id = :equipmentTypeId) " +
                   "AND (:startDate IS NULL OR p.purchaseDate >= :startDate) " +
                   "AND (:endDate IS NULL OR p.purchaseDate <= :endDate)",
           countQuery = "SELECT COUNT(p) FROM Purchase p " +
                        "WHERE (:baseId IS NULL OR p.base.id = :baseId) " +
                        "AND (:equipmentTypeId IS NULL OR p.equipmentType.id = :equipmentTypeId) " +
                        "AND (:startDate IS NULL OR p.purchaseDate >= :startDate) " +
                        "AND (:endDate IS NULL OR p.purchaseDate <= :endDate)")
    Page<Purchase> findFilteredPaged(
            @Param("baseId") Long baseId,
            @Param("equipmentTypeId") Long equipmentTypeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable);

    @Query("SELECT p FROM Purchase p JOIN FETCH p.base JOIN FETCH p.equipmentType JOIN FETCH p.createdBy WHERE p.id = :id")
    java.util.Optional<Purchase> findByIdWithDetails(@Param("id") Long id);
}
