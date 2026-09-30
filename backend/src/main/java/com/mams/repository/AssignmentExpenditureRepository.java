package com.mams.repository;

import com.mams.model.AssignmentExpenditure;
import com.mams.model.enums.AssignmentStatus;
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
public interface AssignmentExpenditureRepository extends JpaRepository<AssignmentExpenditure, Long>, JpaSpecificationExecutor<AssignmentExpenditure> {

    @Query("SELECT COALESCE(SUM(a.quantity), 0L) FROM AssignmentExpenditure a " +
           "WHERE a.status = :status " +
           "AND a.expendedDate BETWEEN :start AND :end " +
           "AND (:baseId IS NULL OR a.base.id = :baseId) " +
           "AND (:equipmentTypeId IS NULL OR a.equipmentType.id = :equipmentTypeId)")
    Long sumExpendedInRange(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end,
            @Param("baseId") Long baseId,
            @Param("equipmentTypeId") Long equipmentTypeId,
            @Param("status") AssignmentStatus status);

    @Query("SELECT COALESCE(SUM(a.quantity), 0L) FROM AssignmentExpenditure a " +
           "WHERE a.status = :status " +
           "AND a.expendedDate < :date " +
           "AND (:baseId IS NULL OR a.base.id = :baseId) " +
           "AND (:equipmentTypeId IS NULL OR a.equipmentType.id = :equipmentTypeId)")
    Long sumExpendedBeforeDate(
            @Param("date") LocalDate date,
            @Param("baseId") Long baseId,
            @Param("equipmentTypeId") Long equipmentTypeId,
            @Param("status") AssignmentStatus status);

    @Query("SELECT COALESCE(SUM(a.quantity), 0L) FROM AssignmentExpenditure a " +
           "WHERE a.status = :status " +
           "AND (:baseId IS NULL OR a.base.id = :baseId) " +
           "AND (:equipmentTypeId IS NULL OR a.equipmentType.id = :equipmentTypeId)")
    Long sumCurrentlyAssigned(
            @Param("baseId") Long baseId,
            @Param("equipmentTypeId") Long equipmentTypeId,
            @Param("status") AssignmentStatus status);

    @Query("SELECT a FROM AssignmentExpenditure a WHERE (:baseId IS NULL OR a.base.id = :baseId) " +
           "AND (:equipmentTypeId IS NULL OR a.equipmentType.id = :equipmentTypeId) " +
           "AND (:status IS NULL OR a.status = :status) " +
           "AND (:startDate IS NULL OR a.assignedDate >= :startDate) " +
           "AND (:endDate IS NULL OR a.assignedDate <= :endDate)")
    List<AssignmentExpenditure> findFiltered(
            @Param("baseId") Long baseId,
            @Param("equipmentTypeId") Long equipmentTypeId,
            @Param("status") AssignmentStatus status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT a FROM AssignmentExpenditure a WHERE (:baseId IS NULL OR a.base.id = :baseId) " +
           "AND (:equipmentTypeId IS NULL OR a.equipmentType.id = :equipmentTypeId) " +
           "AND (:status IS NULL OR a.status = :status) " +
           "AND (:startDate IS NULL OR a.assignedDate >= :startDate) " +
           "AND (:endDate IS NULL OR a.assignedDate <= :endDate)")
    Page<AssignmentExpenditure> findFilteredPaged(
            @Param("baseId") Long baseId,
            @Param("equipmentTypeId") Long equipmentTypeId,
            @Param("status") AssignmentStatus status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable);

    @Query(value = "SELECT a FROM AssignmentExpenditure a " +
                   "JOIN FETCH a.base " +
                   "JOIN FETCH a.equipmentType " +
                   "JOIN FETCH a.createdBy " +
                   "WHERE (:baseId IS NULL OR a.base.id = :baseId) " +
                   "AND (:equipmentTypeId IS NULL OR a.equipmentType.id = :equipmentTypeId) " +
                   "AND (:status IS NULL OR a.status = :status) " +
                   "AND (:startDate IS NULL OR a.assignedDate >= :startDate) " +
                   "AND (:endDate IS NULL OR a.assignedDate <= :endDate)",
           countQuery = "SELECT COUNT(a) FROM AssignmentExpenditure a " +
                        "WHERE (:baseId IS NULL OR a.base.id = :baseId) " +
                        "AND (:equipmentTypeId IS NULL OR a.equipmentType.id = :equipmentTypeId) " +
                        "AND (:status IS NULL OR a.status = :status) " +
                        "AND (:startDate IS NULL OR a.assignedDate >= :startDate) " +
                        "AND (:endDate IS NULL OR a.assignedDate <= :endDate)")
    Page<AssignmentExpenditure> findAssignmentsPaged(
            @Param("baseId") Long baseId,
            @Param("equipmentTypeId") Long equipmentTypeId,
            @Param("status") AssignmentStatus status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable);

    @Query("SELECT a FROM AssignmentExpenditure a JOIN FETCH a.base JOIN FETCH a.equipmentType JOIN FETCH a.createdBy WHERE a.id = :id")
    java.util.Optional<AssignmentExpenditure> findByIdWithDetails(@Param("id") Long id);
}
