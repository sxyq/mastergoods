package com.zhihuiji.backend.application.service;

import com.zhihuiji.backend.domain.entity.SessionEntity;
import com.zhihuiji.backend.domain.entity.UserEntity;
import com.zhihuiji.backend.infrastructure.repository.SessionRepository;
import com.zhihuiji.backend.infrastructure.repository.UserRepository;
import com.zhihuiji.backend.infrastructure.security.TokenService;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private static final long EXPIRE_MILLIS = 7L * 24L * 60L * 60L * 1000L;
    private static final long VERIFY_CODE_EXPIRE_MILLIS = 10L * 60L * 1000L;

    private final String inviteCode;
    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final SessionAccessService sessionAccessService;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, VerificationCodeEntry> verificationCodes = new ConcurrentHashMap<>();

    public AuthService(
        @Value("${auth.invite-code:}") String inviteCode,
        UserRepository userRepository,
        SessionRepository sessionRepository,
        SessionAccessService sessionAccessService,
        PasswordEncoder passwordEncoder,
        TokenService tokenService
    ) {
        this.inviteCode = inviteCode == null ? "" : inviteCode.trim();
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.sessionAccessService = sessionAccessService;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @Transactional
    public AuthResult register(String phone, String password, String verifyCode) {
        if (!isVerifyCodeValid(phone, "register", verifyCode)) {
            throw new IllegalArgumentException("verify code is invalid");
        }
        if (userRepository.findByPhone(phone).isPresent()) {
            throw new IllegalArgumentException("phone already registered");
        }
        long now = System.currentTimeMillis();
        UserEntity user = new UserEntity();
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setNickname("User" + phone.substring(Math.max(0, phone.length() - 4)));
        user.setStatus(1);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user = userRepository.save(user);
        SessionEntity session = createSession(user.getId(), now);
        return new AuthResult(user.getId(), session.getToken(), session.getRefreshToken(), (int) (EXPIRE_MILLIS / 1000L));
    }

    @Transactional
    public AuthResult login(String phone, String password) {
        UserEntity user = userRepository.findByPhone(phone)
            .orElseThrow(() -> new IllegalArgumentException("account not found"));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("phone or password is incorrect");
        }
        SessionEntity session = createSession(user.getId(), System.currentTimeMillis());
        return new AuthResult(user.getId(), session.getToken(), session.getRefreshToken(), (int) (EXPIRE_MILLIS / 1000L));
    }

    @Transactional
    public AuthResult refresh(String refreshToken) {
        SessionEntity old = sessionAccessService.findActiveSessionByRefreshToken(refreshToken)
            .orElseThrow(() -> new IllegalArgumentException("refresh token is invalid"));
        old.setIsActive(false);
        sessionRepository.save(old);
        sessionAccessService.invalidateSession(old);
        SessionEntity session = createSession(old.getUserId(), System.currentTimeMillis());
        return new AuthResult(old.getUserId(), session.getToken(), session.getRefreshToken(), (int) (EXPIRE_MILLIS / 1000L));
    }

    @Transactional
    public void logout(String token) {
        Optional<SessionEntity> session = sessionAccessService.findActiveSessionByToken(token);
        session.ifPresent(value -> {
            value.setIsActive(false);
            sessionRepository.save(value);
            sessionAccessService.invalidateSession(value);
        });
    }

    public UserProfile me(String token) {
        SessionEntity session = sessionAccessService.findActiveSessionByToken(token)
            .orElseThrow(() -> new IllegalArgumentException("not logged in"));
        if (session.getExpiresAt() < System.currentTimeMillis()) {
            throw new IllegalArgumentException("session expired");
        }
        UserEntity user = userRepository.findById(session.getUserId())
            .orElseThrow(() -> new IllegalArgumentException("user not found"));
        return new UserProfile(user.getId(), user.getPhone(), user.getNickname(), user.getStatus());
    }

    private SessionEntity createSession(Long userId, long now) {
        SessionEntity session = new SessionEntity();
        session.setUserId(userId);
        session.setToken(tokenService.issueToken());
        session.setRefreshToken(tokenService.issueToken());
        session.setExpiresAt(now + EXPIRE_MILLIS);
        session.setIsActive(true);
        session.setCreatedAt(now);
        session = sessionRepository.save(session);
        sessionAccessService.cacheSession(session);
        return session;
    }

    public VerifyCodeResult issueVerifyCode(String phone, String type) {
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("phone is required");
        }
        String normalizedType = normalizeVerifyType(type);
        if (!inviteCode.isBlank()) {
            long expireAt = System.currentTimeMillis() + VERIFY_CODE_EXPIRE_MILLIS;
            verificationCodes.put(buildVerifyKey(phone, normalizedType), new VerificationCodeEntry(inviteCode, expireAt));
            return new VerifyCodeResult(true, (int) (VERIFY_CODE_EXPIRE_MILLIS / 1000L));
        }
        String code = String.format("%06d", secureRandom.nextInt(1_000_000));
        long expireAt = System.currentTimeMillis() + VERIFY_CODE_EXPIRE_MILLIS;
        verificationCodes.put(buildVerifyKey(phone, normalizedType), new VerificationCodeEntry(code, expireAt));
        return new VerifyCodeResult(true, (int) (VERIFY_CODE_EXPIRE_MILLIS / 1000L));
    }

    private boolean isVerifyCodeValid(String phone, String type, String verifyCode) {
        if (!inviteCode.isBlank()) {
            return inviteCode.equals(verifyCode);
        }
        String key = buildVerifyKey(phone, normalizeVerifyType(type));
        VerificationCodeEntry entry = verificationCodes.get(key);
        if (entry == null || entry.expiresAt() <= System.currentTimeMillis()) {
            verificationCodes.remove(key);
            return false;
        }
        boolean matched = entry.code().equals(verifyCode);
        if (matched) {
            verificationCodes.remove(key);
        }
        return matched;
    }

    private String buildVerifyKey(String phone, String type) {
        return phone.trim() + "#" + type;
    }

    private String normalizeVerifyType(String type) {
        return (type == null || type.isBlank()) ? "register" : type.trim().toLowerCase();
    }

    public record AuthResult(Long userId, String token, String refreshToken, int expiresIn) {}

    public record UserProfile(Long id, String phone, String nickname, Integer status) {}

    public record VerifyCodeResult(boolean success, int expireSeconds) {}

    private record VerificationCodeEntry(String code, long expiresAt) {}
}
