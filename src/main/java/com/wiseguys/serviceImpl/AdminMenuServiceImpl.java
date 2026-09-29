package com.wiseguys.serviceImpl;

import com.wiseguys.dto.AdminMenuCategoryResponse;
import com.wiseguys.dto.AdminMenuItemResponse;
import com.wiseguys.dto.AdminMenuItemUpsertRequest;
import com.wiseguys.entity.MenuCategory;
import com.wiseguys.entity.MenuItem;
import com.wiseguys.repository.MenuCategoryRepository;
import com.wiseguys.repository.MenuItemRepository;
import com.wiseguys.serviceI.AdminMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminMenuServiceImpl implements AdminMenuService {
    private final MenuCategoryRepository menuCategoryRepository;
    private final MenuItemRepository menuItemRepository;

    @Override
    public List<AdminMenuCategoryResponse> getMenuForAvailabilityToggle() {
        List<MenuCategory> categories = menuCategoryRepository.findAllByOrderByDisplayOrderAsc();

        return categories.stream()
                .map(category -> new AdminMenuCategoryResponse(
                        category.getId(),
                        category.getName(),
                        category.getItems().stream()
                                .filter(item -> !item.isDeleted())
                                .map(this::toItemResponse)
                                .collect(Collectors.toList())
                ))
                .filter(response -> !response.getItems().isEmpty())
                .collect(Collectors.toList());
    }

    @Override
    public AdminMenuItemResponse updateDailyAvailability(Long menuItemId, boolean dailyAvailable) {
        MenuItem item = getNonDeletedItem(menuItemId);
        item.setDailyAvailable(dailyAvailable);
        item = menuItemRepository.save(item);
        return toItemResponse(item);
    }

    @Override
    public AdminMenuItemResponse updateItemActive(Long menuItemId, boolean available) {
        MenuItem item = getNonDeletedItem(menuItemId);
        item.setAvailable(available);
        item = menuItemRepository.save(item);
        return toItemResponse(item);
    }

    @Override
    public AdminMenuItemResponse createMenuItem(AdminMenuItemUpsertRequest request) {
        validateUpsertRequest(request);
        MenuCategory category = resolveCategory(request);

        MenuItem item = new MenuItem();
        item.setCategory(category);
        applyUpsertFields(item, request);
        item.setAvailable(true);
        item.setDailyAvailable(true);
        item.setImageUrl(null);

        item = menuItemRepository.save(item);
        return toItemResponse(item);
    }

    @Override
    public AdminMenuItemResponse updateMenuItem(Long menuItemId, AdminMenuItemUpsertRequest request) {
        validateUpsertRequest(request);
        MenuItem item = getNonDeletedItem(menuItemId);

        if (request.getCategoryId() != null || (request.getNewCategoryName() != null && !request.getNewCategoryName().isBlank())) {
            item.setCategory(resolveCategory(request));
        }
        applyUpsertFields(item, request);

        item = menuItemRepository.save(item);
        return toItemResponse(item);
    }

    @Override
    public void deleteMenuItem(Long menuItemId) {
        MenuItem item = getNonDeletedItem(menuItemId);
        if (item.isAvailable()) {
            throw new AdminMenuException("Item must be disabled before it can be deleted: " + menuItemId);
        }
        item.setDeleted(true);
        menuItemRepository.save(item);
    }

    private MenuItem getNonDeletedItem(Long menuItemId) {
        MenuItem item = menuItemRepository.findById(menuItemId)
                .orElseThrow(() -> new AdminMenuException("Menu item not found: " + menuItemId));
        if (item.isDeleted()) {
            throw new AdminMenuException("Menu item not found: " + menuItemId);
        }
        return item;
    }

    private void validateUpsertRequest(AdminMenuItemUpsertRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new AdminMenuException("Item name is required.");
        }
        if (request.getPrice() == null || request.getPrice().signum() < 0) {
            throw new AdminMenuException("A valid, non-negative price is required.");
        }
        boolean hasCategoryId = request.getCategoryId() != null;
        boolean hasNewCategoryName = request.getNewCategoryName() != null && !request.getNewCategoryName().isBlank();
        if (!hasCategoryId && !hasNewCategoryName) {
            throw new AdminMenuException("Either categoryId or newCategoryName must be provided.");
        }
    }

    private MenuCategory resolveCategory(AdminMenuItemUpsertRequest request) {
        if (request.getCategoryId() != null) {
            return menuCategoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new AdminMenuException("Category not found: " + request.getCategoryId()));
        }

        String newName = request.getNewCategoryName().trim();
        MenuCategory category = new MenuCategory();
        category.setName(newName);
        int maxOrder = menuCategoryRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(MenuCategory::getDisplayOrder)
                .max(Comparator.naturalOrder())
                .orElse(0);
        category.setDisplayOrder(maxOrder + 1);
        category.setActive(true);
        return menuCategoryRepository.save(category);
    }

    private void applyUpsertFields(MenuItem item, AdminMenuItemUpsertRequest request) {
        item.setName(request.getName().trim());
        item.setDescription(request.getDescription());
        item.setPrice(request.getPrice());
        item.setSpicy(request.isSpicy());
        item.setJainAvailable(request.isJainAvailable());
        item.setSignature(request.isSignature());
        item.setRequiresVariantSelection(request.isRequiresVariantSelection());
        item.setVariantOptions(request.getVariantOptions());
    }

    private AdminMenuItemResponse toItemResponse(MenuItem item) {
        return new AdminMenuItemResponse(
                item.getId(),
                item.getCategory() != null ? item.getCategory().getId() : null,
                item.getCategory() != null ? item.getCategory().getName() : null,
                item.getName(),
                item.getDescription(),
                item.getPrice(),
                item.isSpicy(),
                item.isJainAvailable(),
                item.isSignature(),
                item.isAvailable(),
                item.isDailyAvailable(),
                item.getImageUrl(),
                item.isRequiresVariantSelection(),
                item.getVariantOptions()
        );
    }

    public static class AdminMenuException extends RuntimeException {
        public AdminMenuException(String message) {
            super(message);
        }
    }
}