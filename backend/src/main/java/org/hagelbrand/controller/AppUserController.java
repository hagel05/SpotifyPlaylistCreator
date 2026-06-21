package org.hagelbrand.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.hagelbrand.config.OAuth2LoginSuccessHandler;
import org.hagelbrand.data.AppUser;
import org.hagelbrand.data.ProviderAccount;
import org.hagelbrand.service.AppUserService;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class AppUserController {

    private final AppUserService appUserService;

    public AppUserController(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    @GetMapping("/me")
    public AppUser getMe(HttpSession session) {
        UUID appUserId = resolveAppUserId(session);
        return appUserService.getUser(appUserId);
    }

    @PutMapping("/me")
    public AppUser updateMe(HttpSession session, @RequestBody Map<String, String> body) {
        UUID appUserId = resolveAppUserId(session);
        return appUserService.updateDisplayName(appUserId, body.get("displayName"));
    }

    @GetMapping("/me/providers")
    public List<ProviderAccount> getLinkedProviders(HttpSession session) {
        UUID appUserId = resolveAppUserId(session);
        return appUserService.getUser(appUserId).linkedProviders();
    }

    @GetMapping("/me/providers/link/{provider}")
    public void initiateProviderLink(
            HttpSession session,
            HttpServletResponse response,
            @PathVariable String provider
    ) throws IOException {
        resolveAppUserId(session); // ensures user is authenticated before starting the OAuth flow
        session.setAttribute(OAuth2LoginSuccessHandler.SESSION_LINK_PROVIDER_INTENT, true);
        response.sendRedirect("/oauth2/authorization/" + provider);
    }

    @DeleteMapping("/me/providers/{provider}")
    public void unlinkProvider(HttpSession session, @PathVariable String provider) {
        UUID appUserId = resolveAppUserId(session);
        appUserService.unlinkProvider(appUserId, provider);
    }

    private UUID resolveAppUserId(HttpSession session) {
        String id = (String) session.getAttribute(OAuth2LoginSuccessHandler.SESSION_APP_USER_ID);
        if (id == null) throw new IllegalStateException("Not authenticated");
        return UUID.fromString(id);
    }
}
