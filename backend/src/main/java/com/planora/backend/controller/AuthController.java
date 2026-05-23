package com.planora.backend.controller;

import com.planora.backend.service.OauthService;
import com.planora.backend.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/auth")
public class AuthController {

    private final OauthService oauthService;
    private final TokenService tokenService;

    @PostMapping(value = "/token/non-expiring", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> generateNonExpiringToken(@AuthenticationPrincipal Jwt jwt) {
        Long userId = tokenService.getUserId(jwt);
        return ResponseEntity.ok(oauthService.generateNonExpiringToken(userId));
    }
}