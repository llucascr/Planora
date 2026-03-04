package com.planora.backend.config;

import com.planora.backend.model.user.Role;
import com.planora.backend.model.user.User;
import com.planora.backend.repository.RoleRepository;
import com.planora.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

@Configuration
public class AdminUserConfig implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminUserConfig.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    public AdminUserConfig(RoleRepository roleRepository, UserRepository userRepository) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        Role roleAdmin = roleRepository.findByName(Role.Values.ADMIN.getDescription());
        Optional<User> userAdmin = userRepository.findByEmail("admin@email.com");

        userAdmin.ifPresentOrElse(
                user -> {
                    log.info("Admin already exist");
                },
                () -> {
                    userRepository.save(User.builder()
                            .login("admin")
                            .avatarUrl("")
                            .email("admin@email.com")
                            .notificationEmail("admin@email.com")
                            .profileUrl("")
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .roles(Set.of(roleAdmin))
                            .build()
                    );
                }
        );

    }

}
