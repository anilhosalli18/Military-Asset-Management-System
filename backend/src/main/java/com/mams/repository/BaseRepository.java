package com.mams.repository;

import com.mams.model.Base;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BaseRepository extends JpaRepository<Base, Long> {
    List<Base> findByIsActiveTrue();
}
