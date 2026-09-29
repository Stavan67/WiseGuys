package com.wiseguys.serviceImpl;

import com.wiseguys.dto.*;
import com.wiseguys.entity.Addon;
import com.wiseguys.entity.MenuCategory;
import com.wiseguys.entity.MenuItem;
import com.wiseguys.repository.AddonRepository;
import com.wiseguys.repository.MenuCategoryRepository;
import com.wiseguys.repository.MenuItemRepository;
import com.wiseguys.serviceI.AdminAddonService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminAddonServiceImpl implements AdminAddonService {

    private final AddonRepository addonRepository;
    private final MenuItemRepository menuItemRepository;
    private final MenuCategoryRepository menuCategoryRepository;

    @Override
    public List<AdminAddonResponse> getAllAddons() {
        return addonRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public AdminAddonResponse createAddon(AddonRequest request) {
        validateAddonRequest(request);
        Addon addon = new Addon();
        addon.setName(request.getName().trim());
        addon.setPrice(request.getPrice());
        addon.setActive(true);
        addon = addonRepository.save(addon);
        return toResponse(addon);
    }

    @Override
    public AdminAddonResponse updateAddon(Long addonId, AddonRequest request) {
        validateAddonRequest(request);
        Addon addon = addonRepository.findById(addonId)
                .orElseThrow(() -> new AdminAddonException("Addon not found: " + addonId));
        addon.setName(request.getName().trim());
        addon.setPrice(request.getPrice());
        addon = addonRepository.save(addon);
        return toResponse(addon);
    }

    @Override
    public AdminAddonResponse setAddonActive(Long addonId, boolean active) {
        Addon addon = addonRepository.findById(addonId)
                .orElseThrow(() -> new AdminAddonException("Addon not found: " + addonId));
        addon.setActive(active);
        addon = addonRepository.save(addon);
        return toResponse(addon);
    }

    @Override
    public AdminAddonAssignmentBoardResponse getAssignmentBoard() {
        List<AdminAddonResponse> allAddons = getAllAddons();

        List<MenuCategory> categories = menuCategoryRepository.findByActiveTrueOrderByDisplayOrderAsc();

        List<AdminAddonAssignmentBoardResponse.CategoryItemsForAddons> categoryRows = categories.stream()
                .map(category -> new AdminAddonAssignmentBoardResponse.CategoryItemsForAddons(
                        category.getId(),
                        category.getName(),
                        category.getItems().stream()
                                .filter(MenuItem::isAvailable)
                                .map(item -> new MenuItemAddonAssignmentResponse(
                                        item.getId(),
                                        item.getName(),
                                        item.getAddons().stream().map(Addon::getId).collect(Collectors.toList())
                                ))
                                .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());

        return new AdminAddonAssignmentBoardResponse(allAddons, categoryRows);
    }

    @Override
    public void updateItemAddons(Long menuItemId, List<Long> addonIds) {
        MenuItem menuItem = menuItemRepository.findById(menuItemId)
                .orElseThrow(() -> new AdminAddonException("Menu item not found: " + menuItemId));

        List<Long> ids = addonIds == null ? List.of() : addonIds;
        List<Addon> addons = addonRepository.findAllById(ids);

        if (addons.size() != ids.size()) {
            throw new AdminAddonException("One or more addon ids were not found.");
        }

        menuItem.setAddons(addons);
        menuItemRepository.save(menuItem);
    }

    private void validateAddonRequest(AddonRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new AdminAddonException("Addon name is required.");
        }
        if (request.getPrice() == null || request.getPrice().signum() < 0) {
            throw new AdminAddonException("Addon price must be zero or greater.");
        }
    }

    private AdminAddonResponse toResponse(Addon addon) {
        return new AdminAddonResponse(addon.getId(), addon.getName(), addon.getPrice(), addon.isActive());
    }

    public static class AdminAddonException extends RuntimeException {
        public AdminAddonException(String message) {
            super(message);
        }
    }
}