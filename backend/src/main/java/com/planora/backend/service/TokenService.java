package com.planora.backend.service;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

    public Long getUserId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }

    public String getGithubToken(Jwt jwt) {
        return jwt.getClaimAsString("githubToken");
    }
}
