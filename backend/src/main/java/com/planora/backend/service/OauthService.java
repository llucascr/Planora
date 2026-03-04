package com.planora.backend.service;

import com.planora.backend.exception.DataNotFoundException;
import com.planora.backend.model.user.Role;
import com.planora.backend.model.user.User;
import com.planora.backend.model.user.dto.UserResponse;
import com.planora.backend.repository.RoleRepository;
import com.planora.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;

@RequiredArgsConstructor
@Service
public class OauthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public void save(OAuth2User user) {

        if (user == null) throw new DataNotFoundException("User not exists");

        String login = user.getAttribute("login");

        if (userRepository.findByLogin(login).isPresent()) {
            throw new DataNotFoundException("User with login " + login + " already exists");
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
