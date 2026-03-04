package com.planora.backend.service;

import com.planora.backend.exception.DataNotFoundException;
import com.planora.backend.model.user.Role;
import com.planora.backend.model.user.User;
import com.planora.backend.model.user.dto.UserResponse;
import com.planora.backend.repository.RoleRepository;
import com.planora.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;

@RequiredArgsConstructor
@Service
public class OauthService {

    private static final Logger log = LoggerFactory.getLogger(OauthService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public void save(OAuth2User user) {

        if (user == null) throw new DataNotFoundException("User not exists");

        if (userRepository.findByLogin(user.getAttribute("login")).isPresent()) {
            log.info("Login successful");
            return;
        }

        Role basicRole = roleRepository.findByName(Role.Values.BASIC.getDescription());

        User newUser = User.builder()
                .login(user.getAttribute("login"))
                .avatarUrl(user.getAttribute("avatar_url"))
                .profileUrl(user.getAttribute("url"))
                .email(user.getAttribute("email"))
                .notificationEmail(user.getAttribute("notification_email"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .roles(Set.of(basicRole))
                .build();

        userRepository.save(newUser);
    }

}
