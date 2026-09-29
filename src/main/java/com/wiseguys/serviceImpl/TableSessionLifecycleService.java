package com.wiseguys.serviceImpl;

import com.wiseguys.entity.TableSession;
import com.wiseguys.repository.TableSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TableSessionLifecycleService {
    private final TableSessionRepository tableSessionRepository;

    public void closeSession(TableSession session) {
        session.setStatus(TableSession.SessionStatus.CLOSED);
        session.setClosedAt(LocalDateTime.now());
        tableSessionRepository.save(session);
    }
}