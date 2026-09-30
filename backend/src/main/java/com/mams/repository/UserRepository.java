package com.mams.repository;

import com.mams.model.User;
import com.mams.model.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    List<User> findByBaseId(Long baseId);

    @Query(value = "SELECT u FROM User u LEFT JOIN FETCH u.base " +
                   "WHERE (:role IS NULL OR u.role = :role) " +
                   "AND (:baseId IS NULL OR u.base.id = :baseId) " +
                   "AND (:isActive IS NULL OR u.isActive = :isActive)",
           countQuery = "SELECT COUNT(u) FROM User u " +
                        "WHERE (:role IS NULL OR u.role = :role) " +
                        "AND (:baseId IS NULL OR u.base.id = :baseId) " +
                        "AND (:isActive IS NULL OR u.isActive = :isActive)")
    Page<User> findUsersPaged(
            @Param("role") Role role,
            @Param("baseId") Long baseId,
            @Param("isActive") Boolean isActive,
            Pageable pageable);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.base WHERE u.id = :id")
    Optional<User> findByIdWithBase(@Param("id") Long id);
}
