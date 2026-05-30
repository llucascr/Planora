package com.planora.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomOAuth2SuccessHandler successHandler;
    private final String frontendBaseUrl;

    public SecurityConfig(CustomOAuth2SuccessHandler successHandler,
                          @Value("${app.frontend.base-url}") String frontendBaseUrl) {
        this.successHandler = successHandler;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration config = new CorsConfiguration();
                    config.setAllowedOrigins(List.of(frontendBaseUrl));
                    config.setAllowedMethods(List.of("*"));
                    config.setAllowedHeaders(List.of("*"));
                    config.setAllowCredentials(true);
                    return config;
                }))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/v1/webhook/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(
                        oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .oauth2Login(oauth2 -> oauth2
                        .loginPage(frontendBaseUrl + "/login")
                        .successHandler(successHandler))
                .sessionManagement(
                        session -> session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )
        ;

        return http.build();
    }

}
