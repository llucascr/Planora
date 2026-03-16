package com.planora.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planora.backend.model.user.dto.LoginResponse;
import com.planora.backend.service.OauthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final OauthService oauthService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CustomOAuth2SuccessHandler(OauthService oauthService) {
        this.oauthService = oauthService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User user = (OAuth2User) authentication.getPrincipal();
        LoginResponse loginResponse = oauthService.save(user);

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().print(objectMapper.writeValueAsString(loginResponse));
        response.getWriter().flush();
    }
}