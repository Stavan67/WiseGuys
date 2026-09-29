package com.wiseguys.controller;

import com.wiseguys.dto.CustomerOrderHistoryResponse;
import com.wiseguys.dto.TableSessionStatusResponse;
import com.wiseguys.entity.CafeTable;
import com.wiseguys.entity.MenuCategory;
import com.wiseguys.entity.TableSession;
import com.wiseguys.exception.SessionEndedException;
import com.wiseguys.serviceI.MenuService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;

@Controller
@RequestMapping("/menu")
@RequiredArgsConstructor
public class MenuController {

    public static final String SESSION_COOKIE_NAME = "wg_session";

    private final MenuService menuService;

    @GetMapping
    public String showMenu(@RequestParam("table") String qrToken,
                           @CookieValue(name = SESSION_COOKIE_NAME, required = false) String existingSessionToken,
                           HttpServletResponse response,
                           Model model) {
        CafeTable table = menuService.getTableByQrTokenOrThrow(qrToken);

        TableSession session;
        try {
            session = menuService.resolveOrCreateSession(table, existingSessionToken);
        } catch (SessionEndedException ex) {
            model.addAttribute("message", ex.getMessage());
            return "session-ended";
        }

        issueSessionCookie(response, session);

        model.addAttribute("table", table);
        model.addAttribute("categories", menuService.getActiveCategories());
        return "menu";
    }

    @GetMapping(value = "/session-status", produces = "application/json")
    @ResponseBody
    public TableSessionStatusResponse getSessionStatus(
            @CookieValue(name = SESSION_COOKIE_NAME, required = false) String sessionToken) {
        return menuService.getSessionStatusByToken(sessionToken);
    }

    private void issueSessionCookie(HttpServletResponse response, TableSession session) {
        ResponseCookie cookie = ResponseCookie.from(SESSION_COOKIE_NAME, session.getSessionToken())
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/")
                //.maxAge(Duration.ofHours(TableSession.HARD_CAP_HOURS))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleInvalidTable(IllegalArgumentException ex, Model model) {
        model.addAttribute("message", "This QR code is no longer valid. Please ask a staff member for assistance.");
        return "session-ended";
    }

    @GetMapping(value = "/my-orders", produces = "application/json")
    @ResponseBody
    public List<CustomerOrderHistoryResponse> getMyOrders(
            @CookieValue(name = SESSION_COOKIE_NAME, required = false) String sessionToken) {
        return menuService.getOrderHistoryForSession(sessionToken);
    }
}