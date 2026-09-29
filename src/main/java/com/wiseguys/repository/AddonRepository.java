package com.wiseguys.repository;

import com.wiseguys.entity.Addon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AddonRepository extends JpaRepository<Addon, Long> {
    List<Addon> findByActiveTrueOrderByNameAsc();
}