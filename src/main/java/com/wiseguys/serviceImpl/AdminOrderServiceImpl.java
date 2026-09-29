package com.wiseguys.serviceImpl;

import com.wiseguys.dto.*;
import com.wiseguys.entity.CafeOrder;
import com.wiseguys.entity.CafeTable;
import com.wiseguys.entity.OrderItem;
import com.wiseguys.entity.TableSession;
import com.wiseguys.repository.CafeOrderRepository;
import com.wiseguys.repository.CafeTableRepository;
import com.wiseguys.repository.TableSessionRepository;
import com.wiseguys.repository.spec.OrderSpecifications;
import com.wiseguys.serviceI.AdminOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminOrderServiceImpl implements AdminOrderService {
    private final CafeOrderRepository orderRepository;
    private final TableSessionRepository tableSessionRepository;
    private final CafeTableRepository tableRepository;
    private final TableSessionLifecycleService tableSessionLifecycleService;

    @Override
    public PagedOrderResponse searchOrders(String tab, String customerName, String tableNumber,
                                           LocalDate date, int page, int size) {
        CafeOrder.OrderStatus status = mapTabToStatus(tab);

        Specification<CafeOrder> spec = Specification.where(OrderSpecifications.hasStatus(status));

        if (customerName != null && !customerName.isBlank()) {
            spec = spec.and(OrderSpecifications.customerNameContains(customerName.trim()));
        }
        if (tableNumber != null && !tableNumber.isBlank()) {
            spec = spec.and(OrderSpecifications.tableNumberContains(tableNumber.trim()));
        }
        if (date != null) {
            spec = spec.and(OrderSpecifications.placedOnDate(date));
        }

        Sort.Direction direction = (status == CafeOrder.OrderStatus.PLACED || status == CafeOrder.OrderStatus.ACCEPTED)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), Sort.by(direction, "placedAt"));
        Page<CafeOrder> resultPage = orderRepository.findAll(spec, pageable);

        List<OrderSummaryResponse> content = resultPage.getContent().stream()
                .map(this::toSummary)
                .collect(Collectors.toList());

        return new PagedOrderResponse(
                content,
                resultPage.getNumber(),
                resultPage.getSize(),
                resultPage.getTotalElements(),
                resultPage.getTotalPages()
        );
    }

    @Override
    public OrderDetailResponse getOrderDetail(Long orderId) {
        CafeOrder order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new AdminOrderException("Order not found: " + orderId));
        return toDetail(order);
    }

    @Override
    public OrderDetailResponse updateStatus(Long orderId, String newStatusRaw) {
        CafeOrder order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new AdminOrderException("Order not found: " + orderId));

        CafeOrder.OrderStatus newStatus = parseStatus(newStatusRaw);
        CafeOrder.OrderStatus current = order.getStatus();

        boolean allowed =
                (current == CafeOrder.OrderStatus.PLACED
                        && (newStatus == CafeOrder.OrderStatus.ACCEPTED || newStatus == CafeOrder.OrderStatus.CANCELLED))
                        || (current == CafeOrder.OrderStatus.ACCEPTED
                        && (newStatus == CafeOrder.OrderStatus.SERVED || newStatus == CafeOrder.OrderStatus.CANCELLED));

        if (!allowed) {
            throw new AdminOrderException("Cannot move an order from " + current + " to " + newStatus + ".");
        }

        order.setStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());
        order = orderRepository.save(order);

        if (newStatus == CafeOrder.OrderStatus.SERVED || newStatus == CafeOrder.OrderStatus.CANCELLED) {
            closeSessionIfFullyResolved(order.getTableSession());
        }

        return toDetail(order);
    }

    private void closeSessionIfFullyResolved(TableSession session) {
        boolean hasActiveOrders = orderRepository.existsByTableSessionAndStatusIn(
                session, List.of(CafeOrder.OrderStatus.PLACED, CafeOrder.OrderStatus.ACCEPTED));

        if (!hasActiveOrders && session.getStatus() == TableSession.SessionStatus.OPEN) {
            tableSessionLifecycleService.closeSession(session);
        }
    }

    private CafeOrder.OrderStatus mapTabToStatus(String tab) {
        if (tab == null || tab.isBlank()) {
            throw new AdminOrderException("A status tab is required.");
        }
        switch (tab.trim().toUpperCase()) {
            case "NEW": return CafeOrder.OrderStatus.PLACED;
            case "ACCEPTED": return CafeOrder.OrderStatus.ACCEPTED;
            case "SERVED": return CafeOrder.OrderStatus.SERVED;
            case "CANCELLED": return CafeOrder.OrderStatus.CANCELLED;
            default: throw new AdminOrderException("Invalid tab: " + tab);
        }
    }

    private CafeOrder.OrderStatus parseStatus(String raw) {
        try {
            return CafeOrder.OrderStatus.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AdminOrderException("Invalid status: " + raw);
        }
    }

    private OrderSummaryResponse toSummary(CafeOrder order) {
        int itemCount = order.getItems().stream().mapToInt(OrderItem::getQuantity).sum();
        return new OrderSummaryResponse(
                order.getId(),
                order.getTableSession().getTable().getTableNumber(),
                order.getCustomer().getName(),
                order.getCustomer().getPhoneNumber(),
                order.getStatus().name(),
                itemCount,
                order.getTotalAmount(),
                order.getPlacedAt()
        );
    }

    private OrderDetailResponse toDetail(CafeOrder order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(i -> {
                    List<OrderItemAddonResponse> addonResponses = i.getSelectedAddons().stream()
                            .map(a -> new OrderItemAddonResponse(
                                    a.getAddonName(),
                                    a.getUnitPrice(),
                                    a.getQuantity(),
                                    a.getUnitPrice().multiply(BigDecimal.valueOf(a.getQuantity()))
                            ))
                            .collect(Collectors.toList());

                    BigDecimal addonsTotal = addonResponses.stream()
                            .map(OrderItemAddonResponse::getLineTotal)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal lineTotal = i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity()))
                            .add(addonsTotal);

                    return new OrderItemResponse(
                            i.getItemName(),
                            i.getUnitPrice(),
                            i.getQuantity(),
                            i.getSpecialNote(),
                            lineTotal,
                            i.getSelectedVariant(),
                            addonResponses
                    );
                })
                .collect(Collectors.toList());

        return new OrderDetailResponse(
                order.getId(),
                order.getTableSession().getTable().getTableNumber(),
                order.getCustomer().getName(),
                order.getCustomer().getPhoneNumber(),
                order.getStatus().name(),
                order.getSubtotalAmount(),
                order.getCgstAmount(),
                order.getSgstAmount(),
                order.getTotalAmount(),
                order.getPlacedAt(),
                order.getUpdatedAt(),
                items
        );
    }

    public static class AdminOrderException extends RuntimeException {
        public AdminOrderException(String message) {
            super(message);
        }
    }

    @Override
    @Transactional
    public void closeTable(String tableNumber) {
        List<CafeOrder> activeOrders = orderRepository.findByTableSession_Table_TableNumberAndStatusIn(
                tableNumber, List.of(CafeOrder.OrderStatus.PLACED, CafeOrder.OrderStatus.ACCEPTED));

        if (activeOrders.isEmpty()) {
            throw new AdminOrderException("No active orders found for table " + tableNumber + ".");
        }

        LocalDateTime now = LocalDateTime.now();
        TableSession session = activeOrders.get(0).getTableSession();

        for (CafeOrder order : activeOrders) {
            order.setStatus(CafeOrder.OrderStatus.SERVED);
            order.setUpdatedAt(now);
        }
        orderRepository.saveAll(activeOrders);

        if (session.getStatus() == TableSession.SessionStatus.OPEN) {
            tableSessionLifecycleService.closeSession(session);
        }
    }

    @Override
    public Set<String> getTablesWithPendingNewOrders() {
        return orderRepository.findByStatus(CafeOrder.OrderStatus.PLACED).stream()
                .map(o -> o.getTableSession().getTable().getTableNumber())
                .collect(Collectors.toSet());
    }
}