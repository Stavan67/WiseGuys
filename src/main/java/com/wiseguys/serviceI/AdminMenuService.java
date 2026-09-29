package com.wiseguys.serviceI;

import com.wiseguys.dto.AdminMenuCategoryResponse;
import com.wiseguys.dto.AdminMenuItemResponse;
import com.wiseguys.dto.AdminMenuItemUpsertRequest;

import java.util.List;

public interface AdminMenuService {
    List<AdminMenuCategoryResponse> getMenuForAvailabilityToggle();

    AdminMenuItemResponse updateDailyAvailability(Long menuItemId, boolean dailyAvailable);

    AdminMenuItemResponse updateItemActive(Long menuItemId, boolean available);

    AdminMenuItemResponse createMenuItem(AdminMenuItemUpsertRequest request);

    AdminMenuItemResponse updateMenuItem(Long menuItemId, AdminMenuItemUpsertRequest request);

    void deleteMenuItem(Long menuItemId);
}