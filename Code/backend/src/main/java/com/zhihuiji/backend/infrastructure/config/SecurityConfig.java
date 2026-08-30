package com.zhihuiji.backend.infrastructure.config;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.zhihuiji.backend.application.service.admin.AdminAuditService;
import com.zhihuiji.backend.infrastructure.security.TokenAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {
    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final TokenAuthenticationFilter tokenAuthenticationFilter;
    private final Environment environment;
    private final List<String> allowedOriginPatterns;
    private final AdminAuditService adminAuditService;

    public SecurityConfig(
        TokenAuthenticationFilter tokenAuthenticationFilter,
        Environment environment,
        @Value("${cors.origin-patterns:http://localhost:*,http://127.0.0.1:*}") String corsOriginPatterns,
        AdminAuditService adminAuditService
    ) {
        this.tokenAuthenticationFilter = tokenAuthenticationFilter;
        this.environment = environment;
        this.allowedOriginPatterns = parseOriginPatterns(corsOriginPatterns);
        this.adminAuditService = adminAuditService;
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
            .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint((request, response, exception) -> {
                if (isAdminPath(request)) {
                    try {
                        adminAuditService.recordSecurityDenial(request, null, "ANONYMOUS", "authentication_required");
                    } catch (RuntimeException ignored) {
                        log.warn("Unable to persist unauthenticated administrator access denial");
                    }
                }
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setContentType("application/json");
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                response.setHeader("Cache-Control", "no-store");
                response.getWriter().write("{\"code\":401,\"message\":\"unauthorized\",\"data\":null,\"timestamp\":"
                    + System.currentTimeMillis() + "}");
            }))
            .addFilterBefore(tokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .httpBasic(AbstractHttpConfigurer::disable);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(allowedOriginPatterns);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
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

    private static boolean isAdminPath(HttpServletRequest request) {
        if (request == null) return false;
        String path = request.getRequestURI();
        return path != null && (path.equals("/v1/admin") || path.startsWith("/v1/admin/")
            || path.equals("/v2/admin") || path.startsWith("/v2/admin/"));
    }

    private static List<String> parseOriginPatterns(String rawPatterns) {
        if (rawPatterns == null || rawPatterns.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(rawPatterns.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
    }
}
