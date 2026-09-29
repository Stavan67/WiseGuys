package com.wiseguys.serviceI;

import com.wiseguys.dto.CustomerOrderHistoryResponse;
import com.wiseguys.dto.TableSessionStatusResponse;
import com.wiseguys.entity.CafeTable;
import com.wiseguys.entity.MenuCategory;
import com.wiseguys.entity.TableSession;

import java.util.List;

public interface MenuService {
    CafeTable getTableOrThrow(String tableNumber);

    CafeTable getTableByQrTokenOrThrow(String qrToken);

    List<MenuCategory> getActiveCategories();

    TableSessionStatusResponse getSessionStatus(String tableNumber);

    TableSession resolveOrCreateSession(CafeTable table, String existingSessionToken);

    TableSessionStatusResponse getSessionStatusByToken(String sessionToken);

    List<CustomerOrderHistoryResponse> getOrderHistoryForSession(String sessionToken);
}