package com.wiseguys.controller;

import com.wiseguys.dto.*;
import com.wiseguys.serviceI.AdminAddonService;
import com.wiseguys.serviceImpl.AdminAddonServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/addons")
@RequiredArgsConstructor
public class AdminAddonController {

    private final AdminAddonService adminAddonService;

    @GetMapping
    public List<AdminAddonResponse> getAllAddons() {
        return adminAddonService.getAllAddons();
    }

    @PostMapping
    public AdminAddonResponse createAddon(@RequestBody AddonRequest request) {
        return adminAddonService.createAddon(request);
    }

    @PutMapping("/{addonId}")
    public AdminAddonResponse updateAddon(@PathVariable Long addonId, @RequestBody AddonRequest request) {
        return adminAddonService.updateAddon(addonId, request);
    }

    @PatchMapping("/{addonId}/active")
    public AdminAddonResponse setAddonActive(@PathVariable Long addonId, @RequestParam boolean active) {
        return adminAddonService.setAddonActive(addonId, active);
    }

    @GetMapping("/assignment-board")
    public AdminAddonAssignmentBoardResponse getAssignmentBoard() {
        return adminAddonService.getAssignmentBoard();
    }

    @PutMapping("/items/{menuItemId}")
    public ResponseEntity<Void> updateItemAddons(
            @PathVariable Long menuItemId,
            @RequestBody UpdateItemAddonsRequest request) {
        adminAddonService.updateItemAddons(menuItemId, request.getAddonIds());
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(AdminAddonServiceImpl.AdminAddonException.class)
    public ResponseEntity<String> handleError(AdminAddonServiceImpl.AdminAddonException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}