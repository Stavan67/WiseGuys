package com.wiseguys.serviceI;

import com.wiseguys.dto.AddonRequest;
import com.wiseguys.dto.AdminAddonAssignmentBoardResponse;
import com.wiseguys.dto.AdminAddonResponse;

import java.util.List;

public interface AdminAddonService {
    List<AdminAddonResponse> getAllAddons();

    AdminAddonResponse createAddon(AddonRequest request);

    AdminAddonResponse updateAddon(Long addonId, AddonRequest request);

    AdminAddonResponse setAddonActive(Long addonId, boolean active);

    AdminAddonAssignmentBoardResponse getAssignmentBoard();

    void updateItemAddons(Long menuItemId, List<Long> addonIds);
}