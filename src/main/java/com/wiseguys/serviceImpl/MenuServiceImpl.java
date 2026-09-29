package com.wiseguys.serviceImpl;

import com.wiseguys.dto.CustomerOrderHistoryResponse;
import com.wiseguys.dto.TableSessionStatusResponse;
import com.wiseguys.entity.CafeTable;
import com.wiseguys.entity.MenuCategory;
import com.wiseguys.entity.TableSession;
import com.wiseguys.exception.SessionEndedException;
import com.wiseguys.repository.CafeOrderRepository;
import com.wiseguys.repository.CafeTableRepository;
import com.wiseguys.repository.MenuCategoryRepository;
import com.wiseguys.repository.TableSessionRepository;
import com.wiseguys.serviceI.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements MenuService {
    private final CafeTableRepository tableRepository;
    private final MenuCategoryRepository categoryRepository;
    private final TableSessionRepository tableSessionRepository;
    private final TableSessionLifecycleService tableSessionLifecycleService;
    private final CafeOrderRepository orderRepository;

    private static final String SESSION_ENDED_MESSAGE =
            "Your order session has ended. Please scan the table's QR code again to start a new order.";

    @Override
    public CafeTable getTableOrThrow(String tableNumber) {
        return tableRepository.findByTableNumber(tableNumber)
                .orElseThrow(() -> new IllegalArgumentException("Invalid table QR code: " + tableNumber));
    }

    @Override
    public CafeTable getTableByQrTokenOrThrow(String qrToken) {
        return tableRepository.findByQrCodeValue(qrToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or unrecognized QR code."));
    }

    @Override
    public List<MenuCategory> getActiveCategories() {
        return categoryRepository.findByActiveTrueOrderByDisplayOrderAsc();
    }

    @Override
    public TableSessionStatusResponse getSessionStatus(String tableNumber) {
        CafeTable table = getTableOrThrow(tableNumber);

        return tableSessionRepository.findFirstByTableOrderByStartedAtDesc(table)
                .map(session -> {
                    if (session.getStatus() == TableSession.SessionStatus.OPEN && session.isExpired()) {
                        tableSessionLifecycleService.closeSession(session);
                    }

                    boolean active = session.getStatus() == TableSession.SessionStatus.OPEN;
                    String message = active ? null : SESSION_ENDED_MESSAGE;
                    return new TableSessionStatusResponse(active, message);
                })
                .orElseGet(() -> new TableSessionStatusResponse(true, null));
    }

    @Override
    @Transactional
    public TableSession resolveOrCreateSession(CafeTable table, String existingSessionToken) {
        if (existingSessionToken != null && !existingSessionToken.isBlank()) {
            TableSession existing = tableSessionRepository.findBySessionToken(existingSessionToken).orElse(null);

            if (existing != null && existing.getTable().getId().equals(table.getId())) {
                if (existing.getStatus() == TableSession.SessionStatus.OPEN && !existing.isExpired()) {
                    return existing; // page reload/poll no longer resets the no-order clock
                }

                if (existing.getStatus() == TableSession.SessionStatus.OPEN && existing.isExpired()) {
                    tableSessionLifecycleService.closeSession(existing);
                }

                throw new SessionEndedException(SESSION_ENDED_MESSAGE);
            }
        }

        // No cookie, or cookie belongs to a different table — always free to start fresh.
        TableSession newSession = new TableSession();
        newSession.setTable(table);
        newSession.setStatus(TableSession.SessionStatus.OPEN);
        newSession.refreshExpiry();
        return tableSessionRepository.save(newSession);
    }

    @Override
    public TableSessionStatusResponse getSessionStatusByToken(String sessionToken) {
        if (sessionToken == null || sessionToken.isBlank()) {
            return new TableSessionStatusResponse(false, SESSION_ENDED_MESSAGE);
        }

        return tableSessionRepository.findBySessionToken(sessionToken)
                .map(session -> {
                    if (session.getStatus() == TableSession.SessionStatus.OPEN && session.isExpired()) {
                        tableSessionLifecycleService.closeSession(session);
                    }

                    boolean active = session.getStatus() == TableSession.SessionStatus.OPEN;
                    return new TableSessionStatusResponse(active, active ? null : SESSION_ENDED_MESSAGE);
                })
                .orElseGet(() -> new TableSessionStatusResponse(false, SESSION_ENDED_MESSAGE));
    }

    @Override
    public List<CustomerOrderHistoryResponse> getOrderHistoryForSession(String sessionToken) {
        if (sessionToken == null || sessionToken.isBlank()) {
            return List.of();
        }

        return orderRepository.findByTableSession_SessionTokenOrderByPlacedAtAsc(sessionToken).stream()
                .map(order -> new CustomerOrderHistoryResponse(
                        order.getId(),
                        order.getStatus().name(),
                        order.getPlacedAt(),
                        order.getTotalAmount(),
                        order.getItems().stream()
                                .map(i -> new CustomerOrderHistoryResponse.Item(
                                        i.getItemName(), i.getSelectedVariant(), i.getQuantity(), i.getUnitPrice()))
                                .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());
    }
}