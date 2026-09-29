package com.wiseguys.repository;

import com.wiseguys.entity.CafeTable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CafeTableRepository extends JpaRepository<CafeTable, Long> {
    Optional<CafeTable> findByTableNumber(String tableNumber);
    Optional<CafeTable> findByQrCodeValue(String qrCodeValue);
}