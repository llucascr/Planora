package com.planora.backend.service;

import com.planora.backend.exception.DataNotFoundException;
import com.planora.backend.model.user.Role;
import com.planora.backend.model.user.User;
import com.planora.backend.model.user.dto.LoginResponse;
import com.planora.backend.repository.RoleRepository;
import com.planora.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class OauthService {

    private static final Logger log = LoggerFactory.getLogger(OauthService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    private final JwtEncoder jwtEncoder;

    public LoginResponse save(OAuth2User oAuth2User, String githubToken) {

        if (oAuth2User == null) throw new DataNotFoundException("User not exists");

        Optional<User> userOp = userRepository.findByLogin(oAuth2User.getAttribute("login"));

        if (userOp.isPresent()) {
            User existing = userOp.get();
            existing.setGithubToken(githubToken);
            userRepository.save(existing);
            return generatedLoginResponse(existing);
        }

        Role basicRole = roleRepository.findByName(Role.Values.BASIC.getDescription());

        User user = User.builder()
                .login(oAuth2User.getAttribute("login"))
                .avatarUrl(oAuth2User.getAttribute("avatar_url"))
                .profileUrl(oAuth2User.getAttribute("url"))
                .email(oAuth2User.getAttribute("email"))
                .notificationEmail(oAuth2User.getAttribute("notification_email"))
                .githubToken(githubToken)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .roles(Set.of(basicRole))
                .build();

        userRepository.save(user);
        return generatedLoginResponse(user);
    }

    private LoginResponse generatedLoginResponse(User user) {
        Instant now = Instant.now();
        long expiresIn = 860000L;

        String scopes = user.getRoles()
                .stream()
                .map(Role::getName)
                .collect(Collectors.joining(" "));

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("Planora")
                .subject(user.getUserId().toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expiresIn))
                .claim("scope", scopes)
                .claim("githubToken", user.getGithubToken())
                .build();

        String jwtValue = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();

        log.info("Login efetuado");
        return new LoginResponse(jwtValue, expiresIn);
    }

}
