package com.wiseguys.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "table_session")
@Data
public class TableSession {
    public static final long INACTIVITY_TIMEOUT_MINUTES = 45;
    public static final long HARD_CAP_HOURS = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_id", nullable = false)
    private CafeTable table;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status = SessionStatus.OPEN;

    @Column(name = "session_token", nullable = false, unique = true, updatable = false)
    private String sessionToken = UUID.randomUUID().toString();

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt = LocalDateTime.now();

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "last_activity_at", nullable = false)
    private LocalDateTime lastActivityAt = LocalDateTime.now();

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(INACTIVITY_TIMEOUT_MINUTES);

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public void refreshExpiry() {
        LocalDateTime now = LocalDateTime.now();
        this.lastActivityAt = now;
        LocalDateTime inactivityDeadline = now.plusMinutes(INACTIVITY_TIMEOUT_MINUTES);
        LocalDateTime hardCapDeadline = this.startedAt.plusHours(HARD_CAP_HOURS);
        this.expiresAt = inactivityDeadline.isBefore(hardCapDeadline) ? inactivityDeadline : hardCapDeadline;
    }

    public enum SessionStatus { OPEN, CLOSED }
}