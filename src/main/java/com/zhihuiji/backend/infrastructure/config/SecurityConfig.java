package com.zhihuiji.backend.infrastructure.config;

import jakarta.servlet.DispatcherType;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import com.zhihuiji.backend.infrastructure.security.TokenAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

    private final TokenAuthenticationFilter tokenAuthenticationFilter;
    private final Environment environment;
    private final List<String> allowedOriginPatterns;

    public SecurityConfig(
        TokenAuthenticationFilter tokenAuthenticationFilter,
        Environment environment,
        @Value("${cors.origin-patterns:http://localhost:*,http://127.0.0.1:*}") String corsOriginPatterns
    ) {
        this.tokenAuthenticationFilter = tokenAuthenticationFilter;
        this.environment = environment;
        this.allowedOriginPatterns = parseOriginPatterns(corsOriginPatterns);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        boolean localProfile = environment.matchesProfiles("local");
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> {})
            .authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR, DispatcherType.FORWARD).permitAll()
                .requestMatchers("/v1/auth/login", "/v1/auth/register", "/v1/auth/refresh", "/v1/auth/verify-code").permitAll()
                .requestMatchers("/v2/auth/login", "/v2/auth/register", "/v2/auth/refresh", "/v2/auth/verify-code").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/v1/admin/**").access((authentication, context) ->
                    new org.springframework.security.authorization.AuthorizationDecision(
                        localProfile || isAuthenticated(authentication.get())
                    )
                )
                .anyRequest().authenticated()
            )
            .addFilterBefore(tokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .httpBasic(AbstractHttpConfigurer::disable);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(allowedOriginPatterns);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("*"));
        configuration.setAllowCredentials(false);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private static boolean isAuthenticated(org.springframework.security.core.Authentication authentication) {
        return authentication != null
            && authentication.isAuthenticated()
            && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private static List<String> parseOriginPatterns(String rawPatterns) {
        if (rawPatterns == null || rawPatterns.isBlank()) {
            return List.of();
        }
        String[] parts = rawPatterns.split(",");
        List<String> patterns = new ArrayList<>(parts.length);
        for (String part : parts) {
            String pattern = part.trim();
            if (!pattern.isEmpty()) {
                patterns.add(pattern);
            }
        }
        return List.copyOf(patterns);
    }
}
