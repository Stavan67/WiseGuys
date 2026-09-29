package com.wiseguys.repository;

import com.wiseguys.entity.CafeTable;
import com.wiseguys.entity.TableSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TableSessionRepository extends JpaRepository<TableSession, Long> {
    Optional<TableSession> findFirstByTableAndStatusOrderByStartedAtDesc(
            CafeTable table, TableSession.SessionStatus status);

    Optional<TableSession> findFirstByTableOrderByStartedAtDesc(CafeTable table);

    Optional<TableSession> findBySessionToken(String sessionToken);

    List<TableSession> findByStatus(TableSession.SessionStatus status);
}