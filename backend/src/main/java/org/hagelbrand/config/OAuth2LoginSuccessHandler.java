package org.hagelbrand.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.hagelbrand.data.AppUser;
import org.hagelbrand.service.AppUserService;
import org.hagelbrand.service.DuplicateEmailException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2LoginSuccessHandler.class);
    public static final String SESSION_APP_USER_ID = "appUserId";
    public static final String SESSION_LINK_PROVIDER_INTENT = "linkProviderIntent";

    private final AppUserService appUserService;
    private final SavedRequestAwareAuthenticationSuccessHandler delegate =
            new SavedRequestAwareAuthenticationSuccessHandler();

    public OAuth2LoginSuccessHandler(AppUserService appUserService) {
        this.appUserService = appUserService;
        this.delegate.setDefaultTargetUrl("/");
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        if (authentication instanceof OAuth2AuthenticationToken token) {
            OAuth2User oAuth2User = token.getPrincipal();
            String provider = token.getAuthorizedClientRegistrationId();
            String providerUserId = oAuth2User.getName();
            String displayName = oAuth2User.getAttribute("display_name");
            String email = oAuth2User.getAttribute("email");

            HttpSession session = request.getSession();
            boolean isLinkIntent = Boolean.TRUE.equals(session.getAttribute(SESSION_LINK_PROVIDER_INTENT));
            session.removeAttribute(SESSION_LINK_PROVIDER_INTENT);

            if (isLinkIntent) {
                String existingUserIdStr = (String) session.getAttribute(SESSION_APP_USER_ID);
                if (existingUserIdStr == null) {
                    log.warn("Link intent present but no session user — session may have expired");
                    response.sendRedirect("/?error=link_session_expired");
                    return;
                }
                UUID existingUserId = UUID.fromString(existingUserIdStr);
                try {
                    appUserService.linkProvider(existingUserId, provider, providerUserId, email);
                    log.info("Linked provider {} to user {}", provider, existingUserId);
                } catch (IllegalStateException e) {
                    log.warn("Provider link failed: {}", e.getMessage());
                    response.sendRedirect("/?error=link_failed&reason="
                            + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8));
                    return;
                }
            } else {
                try {
                    AppUser appUser = appUserService.findOrCreateByProvider(
                            provider, providerUserId, email, displayName);
                    session.setAttribute(SESSION_APP_USER_ID, appUser.appUserId().toString());
                    log.info("OAuth2 login success: provider={} appUserId={}", provider, appUser.appUserId());
                } catch (DuplicateEmailException e) {
                    log.warn("Login blocked: email already linked via {}", e.getExistingProvider());
                    response.sendRedirect("/?error=email_conflict&existingProvider="
                            + URLEncoder.encode(e.getExistingProvider(), StandardCharsets.UTF_8));
                    return;
                }
            }
        }

        delegate.onAuthenticationSuccess(request, response, authentication);
    }
}
