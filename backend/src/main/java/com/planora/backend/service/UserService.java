package com.planora.backend.service;

import com.planora.backend.model.user.User;
import com.planora.backend.model.user.dto.UserResponse;
import com.planora.backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;

    public User findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
    }

    public User findByLogin(String login) {
        return userRepository.findByLogin(login)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + login));
    }

    public Optional<User> findOptionalByLogin(String login) {
        return userRepository.findByLogin(login);
    }

}
