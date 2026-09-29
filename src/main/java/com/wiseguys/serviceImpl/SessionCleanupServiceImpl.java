package com.wiseguys.serviceImpl;

import com.wiseguys.entity.TableSession;
import com.wiseguys.repository.TableSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SessionCleanupServiceImpl {

    private final TableSessionRepository tableSessionRepository;
    private final TableSessionLifecycleService tableSessionLifecycleService;

    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void closeExpiredSessions() {
        List<TableSession> openSessions = tableSessionRepository.findByStatus(TableSession.SessionStatus.OPEN);
        for (TableSession session : openSessions) {
            if (session.isExpired()) {
                tableSessionLifecycleService.closeSession(session);
            }
        }
    }
}