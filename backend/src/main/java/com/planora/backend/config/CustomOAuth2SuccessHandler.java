package com.planora.backend.config;

import com.planora.backend.model.user.dto.LoginResponse;
import com.planora.backend.service.OauthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final OauthService oauthService;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final String frontendBaseUrl;

    public CustomOAuth2SuccessHandler(OauthService oauthService,
                                      OAuth2AuthorizedClientService authorizedClientService,
                                      @Value("${app.frontend.base-url}") String frontendBaseUrl) {
        this.oauthService = oauthService;
        this.authorizedClientService = authorizedClientService;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User user = oauthToken.getPrincipal();

        OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
                oauthToken.getAuthorizedClientRegistrationId(),
                oauthToken.getName()
        );

        String githubToken = authorizedClient.getAccessToken().getTokenValue();
        LoginResponse loginResponse = oauthService.save(user, githubToken);

        String token = loginResponse.accessToken();
        response.sendRedirect(frontendBaseUrl + "/callback/" + token);
    }
}