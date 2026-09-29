package com.wiseguys.controller;

import com.wiseguys.dto.AdminMenuCategoryResponse;
import com.wiseguys.dto.AdminMenuItemResponse;
import com.wiseguys.dto.AdminMenuItemUpsertRequest;
import com.wiseguys.dto.UpdateDailyAvailabilityRequest;
import com.wiseguys.dto.UpdateItemActiveRequest;
import com.wiseguys.serviceI.AdminMenuService;
import com.wiseguys.serviceImpl.AdminMenuServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/menu")
@RequiredArgsConstructor
public class AdminMenuController {
    private final AdminMenuService adminMenuService;

    @GetMapping("/items")
    public List<AdminMenuCategoryResponse> getMenuItems() {
        return adminMenuService.getMenuForAvailabilityToggle();
    }

    @PatchMapping("/items/{menuItemId}/daily-availability")
    public AdminMenuItemResponse updateDailyAvailability(
            @PathVariable Long menuItemId,
            @RequestBody UpdateDailyAvailabilityRequest request) {
        return adminMenuService.updateDailyAvailability(menuItemId, request.isDailyAvailable());
    }

    @PatchMapping("/items/{menuItemId}/active")
    public AdminMenuItemResponse updateItemActive(
            @PathVariable Long menuItemId,
            @RequestBody UpdateItemActiveRequest request) {
        return adminMenuService.updateItemActive(menuItemId, request.isAvailable());
    }

    @PostMapping("/items")
    public AdminMenuItemResponse createMenuItem(@RequestBody AdminMenuItemUpsertRequest request) {
        return adminMenuService.createMenuItem(request);
    }

    @PutMapping("/items/{menuItemId}")
    public AdminMenuItemResponse updateMenuItem(
            @PathVariable Long menuItemId,
            @RequestBody AdminMenuItemUpsertRequest request) {
        return adminMenuService.updateMenuItem(menuItemId, request);
    }

    @DeleteMapping("/items/{menuItemId}")
    public ResponseEntity<Void> deleteMenuItem(@PathVariable Long menuItemId) {
        adminMenuService.deleteMenuItem(menuItemId);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(AdminMenuServiceImpl.AdminMenuException.class)
    public ResponseEntity<String> handleError(AdminMenuServiceImpl.AdminMenuException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}