package com.wiseguys.serviceImpl;

import com.wiseguys.dto.AddonSelectionRequest;
import com.wiseguys.dto.OrderItemRequest;
import com.wiseguys.dto.PlaceOrderRequest;
import com.wiseguys.dto.PlaceOrderResponse;
import com.wiseguys.entity.*;
import com.wiseguys.repository.*;
import com.wiseguys.serviceI.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private static final BigDecimal CGST_RATE = new BigDecimal("0.025");
    private static final BigDecimal SGST_RATE = new BigDecimal("0.025");
    private static final String SESSION_ENDED_MESSAGE =
            "Your session has ended. Please scan the table's QR code again.";

    private final CafeTableRepository tableRepository;
    private final TableSessionRepository tableSessionRepository;
    private final CustomerRepository customerRepository;
    private final MenuItemRepository menuItemRepository;
    private final AddonRepository addonRepository;
    private final CafeOrderRepository orderRepository;
    private final TableSessionLifecycleService tableSessionLifecycleService;

    @Override
    public PlaceOrderResponse placeOrder(PlaceOrderRequest request, String sessionToken) {
        validateRequest(request);

        if (sessionToken == null || sessionToken.isBlank()) {
            throw new OrderValidationException(SESSION_ENDED_MESSAGE);
        }

        TableSession session = tableSessionRepository.findBySessionToken(sessionToken)
                .orElseThrow(() -> new OrderValidationException(SESSION_ENDED_MESSAGE));

        if (session.getStatus() != TableSession.SessionStatus.OPEN || session.isExpired()) {
            if (session.getStatus() == TableSession.SessionStatus.OPEN) {
                tableSessionLifecycleService.closeSession(session);
            }
            throw new OrderValidationException(SESSION_ENDED_MESSAGE);
        }

        CafeTable table = session.getTable();

        if (request.getTableNumber() != null && !request.getTableNumber().isBlank()
                && !request.getTableNumber().trim().equalsIgnoreCase(table.getTableNumber())) {
            throw new OrderValidationException("Table mismatch. Please scan the QR code again.");
        }

        Customer customer = new Customer();
        customer.setName(request.getCustomerName().trim());
        customer.setPhoneNumber(request.getPhoneNumber() == null ? "" : request.getPhoneNumber().trim());
        customer = customerRepository.save(customer);

        CafeOrder order = new CafeOrder();
        order.setTableSession(session);
        order.setCustomer(customer);
        order.setStatus(CafeOrder.OrderStatus.PLACED);

        BigDecimal subtotal = BigDecimal.ZERO;

        for (OrderItemRequest itemReq : request.getItems()) {
            if (itemReq.getQuantity() <= 0) {
                continue;
            }

            MenuItem menuItem = menuItemRepository.findById(itemReq.getMenuItemId())
                    .orElseThrow(() -> new OrderValidationException("Invalid menu item id: " + itemReq.getMenuItemId()));

            if (!menuItem.isAvailable()) {
                throw new OrderValidationException("Item is no longer available: " + menuItem.getName());
            }

            if (!menuItem.isDailyAvailable()) {
                throw new OrderValidationException("Item is currently not available: " + menuItem.getName());
            }

            if (itemReq.getSpecialNote() != null && itemReq.getSpecialNote().length() > 255) {
                throw new OrderValidationException(
                        "Special instructions for " + menuItem.getName() + " must be 255 characters or fewer.");
            }

            String selectedVariant = validateAndResolveVariant(menuItem, itemReq.getSelectedVariant());

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setMenuItem(menuItem);
            orderItem.setItemName(menuItem.getName());
            orderItem.setUnitPrice(menuItem.getPrice());
            orderItem.setQuantity(itemReq.getQuantity());
            orderItem.setSpecialNote(itemReq.getSpecialNote());
            orderItem.setSelectedVariant(selectedVariant);

            BigDecimal itemLineTotal = menuItem.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));

            if (itemReq.getAddons() != null) {
                for (AddonSelectionRequest addonReq : itemReq.getAddons()) {
                    if (addonReq.getQuantity() <= 0) {
                        continue;
                    }

                    Addon addon = addonRepository.findById(addonReq.getAddonId())
                            .orElseThrow(() -> new OrderValidationException("Invalid addon id: " + addonReq.getAddonId()));

                    if (!addon.isActive()) {
                        throw new OrderValidationException("Addon is no longer available: " + addon.getName());
                    }

                    boolean allowedForItem = menuItem.getAddons().stream()
                            .anyMatch(a -> a.getId().equals(addon.getId()));
                    if (!allowedForItem) {
                        throw new OrderValidationException(
                                "Addon '" + addon.getName() + "' is not available for " + menuItem.getName());
                    }

                    OrderItemAddon selectedAddon = new OrderItemAddon();
                    selectedAddon.setOrderItem(orderItem);
                    selectedAddon.setAddonName(addon.getName());
                    selectedAddon.setUnitPrice(addon.getPrice());
                    selectedAddon.setQuantity(addonReq.getQuantity());
                    orderItem.getSelectedAddons().add(selectedAddon);

                    itemLineTotal = itemLineTotal.add(
                            addon.getPrice().multiply(BigDecimal.valueOf(addonReq.getQuantity())));
                }
            }

            order.getItems().add(orderItem);
            subtotal = subtotal.add(itemLineTotal);
        }

        if (order.getItems().isEmpty()) {
            throw new OrderValidationException("Order must contain at least one valid item.");
        }

        BigDecimal cgst = subtotal.multiply(CGST_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal sgst = subtotal.multiply(SGST_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(cgst).add(sgst).setScale(0, RoundingMode.HALF_UP);

        order.setSubtotalAmount(subtotal.setScale(2, RoundingMode.HALF_UP));
        order.setCgstAmount(cgst);
        order.setSgstAmount(sgst);
        order.setTotalAmount(total);
        order.setPlacedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        session.refreshExpiry();
        tableSessionRepository.save(session);

        order = orderRepository.save(order);

        return new PlaceOrderResponse(
                order.getId(),
                order.getStatus().name(),
                order.getSubtotalAmount(),
                order.getCgstAmount(),
                order.getSgstAmount(),
                order.getTotalAmount(),
                session.getExpiresAt()
        );
    }

    private String validateAndResolveVariant(MenuItem menuItem, String requestedVariant) {
        if (!menuItem.isRequiresVariantSelection()) {
            return null;
        }

        if (requestedVariant == null || requestedVariant.isBlank()) {
            throw new OrderValidationException(
                    "Please select an option (e.g. Penne or Spaghetti) for " + menuItem.getName());
        }

        List<String> options = parseVariantOptions(menuItem.getVariantOptions());
        String trimmed = requestedVariant.trim();

        boolean valid = options.stream().anyMatch(opt -> opt.equalsIgnoreCase(trimmed));
        if (!valid) {
            throw new OrderValidationException(
                    "Invalid option '" + requestedVariant + "' for " + menuItem.getName()
                            + ". Choose one of: " + String.join(", ", options));
        }

        return options.stream()
                .filter(opt -> opt.equalsIgnoreCase(trimmed))
                .findFirst()
                .orElse(trimmed);
    }

    private List<String> parseVariantOptions(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return List.of(csv.split(",")).stream().map(String::trim).toList();
    }

    private void validateRequest(PlaceOrderRequest request) {
        if (request.getCustomerName() == null || request.getCustomerName().isBlank()) {
            throw new OrderValidationException("Customer name is required.");
        }
        if (request.getTableNumber() == null || request.getTableNumber().isBlank()) {
            throw new OrderValidationException("Table number is required.");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new OrderValidationException("Order must contain at least one item.");
        }
    }

    public static class OrderValidationException extends RuntimeException {
        public OrderValidationException(String message) {
            super(message);
        }
    }
}