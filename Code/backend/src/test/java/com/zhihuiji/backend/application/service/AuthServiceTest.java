package com.zhihuiji.backend.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import com.zhihuiji.backend.domain.entity.SessionEntity;
import com.zhihuiji.backend.domain.entity.UserEntity;
import com.zhihuiji.backend.infrastructure.repository.SessionRepository;
import com.zhihuiji.backend.infrastructure.repository.UserRepository;
import com.zhihuiji.backend.infrastructure.security.TokenService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private SessionAccessService sessionAccessService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private TokenService tokenService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        authService = new AuthService("", userRepository, sessionRepository, sessionAccessService, passwordEncoder, tokenService);
    }

    @Test
    void registerThrowsWhenVerifyCodeInvalid() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> authService.register("13800138000", "123456", "021218")
        );
        assertEquals("verify code is invalid", ex.getMessage());
    }

    @Test
    void registerThrowsWhenVerifyCodeInvalidAgainstConfiguredInviteCode() {
        authService = new AuthService("021218", userRepository, sessionRepository, sessionAccessService, passwordEncoder, tokenService);

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> authService.register("13800138000", "123456", "bad")
        );

        assertEquals("verify code is invalid", ex.getMessage());
    }

    @Test
    void registerCreatesUserAndActiveSession() {
        authService = new AuthService("021218", userRepository, sessionRepository, sessionAccessService, passwordEncoder, tokenService);
        when(userRepository.findByPhone("13800138000")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("123456")).thenReturn("HASH");
        when(tokenService.issueToken()).thenReturn("token-1", "refresh-1");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            user.setPhone(user.getPhone());
            setEntityId(user, 7L);
            return user;
        });
        when(sessionRepository.save(any(SessionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthService.AuthResult result = authService.register("13800138000", "123456", "021218");

        assertEquals(7L, result.userId());
        assertEquals("token-1", result.token());
        assertEquals("refresh-1", result.refreshToken());
        assertTrue(result.expiresIn() > 0);
        verify(sessionRepository).save(any(SessionEntity.class));
    }

    @Test
    void loginThrowsWhenUserNotFound() {
        when(userRepository.findByPhone("13800138000")).thenReturn(Optional.empty());
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> authService.login("13800138000", "123456")
        );
        assertEquals("account not found", ex.getMessage());
    }

    @Test
    void loginThrowsWhenPasswordInvalid() {
        UserEntity user = new UserEntity();
        user.setPhone("13800138000");
        user.setPasswordHash("HASH");
        user.setStatus(1);
        when(userRepository.findByPhone("13800138000")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("123456", "HASH")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> authService.login("13800138000", "123456")
        );
        assertEquals("phone or password is incorrect", ex.getMessage());
    }

    @Test
    void loginCreatesNewSessionWhenPasswordMatches() {
        UserEntity user = new UserEntity();
        setEntityId(user, 9L);
        user.setPhone("13800138000");
        user.setPasswordHash("HASH");
        user.setStatus(1);
        when(userRepository.findByPhone("13800138000")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("123456", "HASH")).thenReturn(true);
        when(tokenService.issueToken()).thenReturn("token-2", "refresh-2");
        when(sessionRepository.save(any(SessionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthService.AuthResult result = authService.login("13800138000", "123456");

        assertEquals(9L, result.userId());
        assertEquals("token-2", result.token());
        assertEquals("refresh-2", result.refreshToken());
    }

    @Test
    void refreshDeactivatesOldSessionAndCreatesReplacement() {
        SessionEntity old = new SessionEntity();
        old.setUserId(3L);
        old.setRefreshToken("refresh-old");
        old.setToken("token-old");
        old.setIsActive(true);
        old.setExpiresAt(System.currentTimeMillis() + 10000L);
        old.setCreatedAt(1L);
        UserEntity user = new UserEntity();
        setEntityId(user, 3L);
        user.setPhone("13800138003");
        user.setStatus(1);

        when(sessionAccessService.findActiveSessionByRefreshToken("refresh-old")).thenReturn(Optional.of(old));
        when(userRepository.findById(3L)).thenReturn(Optional.of(user));
        when(tokenService.issueToken()).thenReturn("token-new", "refresh-new");
        when(sessionRepository.save(any(SessionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthService.AuthResult result = authService.refresh("refresh-old");

        assertEquals(3L, result.userId());
        assertEquals("token-new", result.token());
        assertEquals("refresh-new", result.refreshToken());
        assertEquals(false, old.getIsActive());
    }

    @Test
    void logoutDeactivatesActiveSessionWhenPresent() {
        SessionEntity session = new SessionEntity();
        session.setUserId(3L);
        session.setToken("token");
        session.setRefreshToken("refresh");
        session.setIsActive(true);
        session.setExpiresAt(System.currentTimeMillis() + 10000L);
        session.setCreatedAt(1L);
        when(sessionAccessService.findActiveSessionByToken("token")).thenReturn(Optional.of(session));

        authService.logout("token");

        assertEquals(false, session.getIsActive());
        verify(sessionRepository).save(session);
    }

    @Test
    void meReturnsProfileForActiveSession() {
        SessionEntity session = new SessionEntity();
        session.setUserId(3L);
        session.setToken("token");
        session.setRefreshToken("refresh");
        session.setIsActive(true);
        session.setExpiresAt(System.currentTimeMillis() + 10000L);
        session.setCreatedAt(1L);
        UserEntity user = new UserEntity();
        setEntityId(user, 3L);
        user.setPhone("13800138000");
        user.setNickname("测试用户");
        user.setStatus(1);

        when(sessionAccessService.findActiveSessionByToken("token")).thenReturn(Optional.of(session));
        when(userRepository.findById(3L)).thenReturn(Optional.of(user));

        AuthService.UserProfile profile = authService.me("token");

        assertEquals(3L, profile.id());
        assertEquals("13800138000", profile.phone());
        assertEquals("测试用户", profile.nickname());
    }

    private static void setEntityId(UserEntity user, Long id) {
        try {
            java.lang.reflect.Field field = UserEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
    }
}
