package com.zhihuiji.backend.application.service;

import com.zhihuiji.backend.domain.entity.SessionEntity;
import com.zhihuiji.backend.infrastructure.repository.SessionRepository;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class SessionAccessService {
    private static final long CACHE_TTL_MILLIS = 30_000L;

    private final SessionRepository sessionRepository;
    private final ConcurrentHashMap<String, CachedSession> accessTokenCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CachedSession> refreshTokenCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> accessTokenBlacklist = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> refreshTokenBlacklist = new ConcurrentHashMap<>();

    public SessionAccessService(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    public Optional<SessionEntity> findActiveSessionByToken(String token) {
        if (token == null || token.isBlank() || isBlacklisted(accessTokenBlacklist, token)) {
            return Optional.empty();
        }
        CachedSession cached = accessTokenCache.get(token);
        if (isCacheValid(cached)) {
            return Optional.of(cached.session());
        }
        Optional<SessionEntity> loaded = sessionRepository.findByTokenAndIsActiveTrue(token)
            .filter(session -> session.getExpiresAt() != null && session.getExpiresAt() > System.currentTimeMillis());
        loaded.ifPresent(this::cacheSession);
        return loaded;
    }

    public Optional<SessionEntity> findActiveSessionByRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank() || isBlacklisted(refreshTokenBlacklist, refreshToken)) {
            return Optional.empty();
        }
        CachedSession cached = refreshTokenCache.get(refreshToken);
        if (isCacheValid(cached)) {
            return Optional.of(cached.session());
        }
        Optional<SessionEntity> loaded = sessionRepository.findByRefreshTokenAndIsActiveTrue(refreshToken)
            .filter(session -> session.getExpiresAt() != null && session.getExpiresAt() > System.currentTimeMillis());
        loaded.ifPresent(this::cacheSession);
        return loaded;
    }

    public void cacheSession(SessionEntity session) {
        if (session == null) return;
        long cachedUntil = Math.min(
            session.getExpiresAt() == null ? System.currentTimeMillis() + CACHE_TTL_MILLIS : session.getExpiresAt(),
            System.currentTimeMillis() + CACHE_TTL_MILLIS
        );
        CachedSession cached = new CachedSession(session, cachedUntil);
        if (session.getToken() != null && !session.getToken().isBlank()) {
            accessTokenCache.put(session.getToken(), cached);
        }
        if (session.getRefreshToken() != null && !session.getRefreshToken().isBlank()) {
            refreshTokenCache.put(session.getRefreshToken(), cached);
        }
    }

    public void invalidateSession(SessionEntity session) {
        if (session == null) return;
        blacklistAccessToken(session.getToken(), session.getExpiresAt());
        blacklistRefreshToken(session.getRefreshToken(), session.getExpiresAt());
        evictSession(session);
    }

    public void blacklistAccessToken(String token, Long expiresAt) {
        blacklist(accessTokenBlacklist, token, expiresAt);
        accessTokenCache.remove(token);
    }

    public void blacklistRefreshToken(String refreshToken, Long expiresAt) {
        blacklist(refreshTokenBlacklist, refreshToken, expiresAt);
        refreshTokenCache.remove(refreshToken);
    }

    private void evictSession(SessionEntity session) {
        accessTokenCache.remove(session.getToken());
        refreshTokenCache.remove(session.getRefreshToken());
    }

    private void blacklist(ConcurrentHashMap<String, Long> blacklist, String token, Long expiresAt) {
        if (token == null || token.isBlank()) return;
        long expireAt = expiresAt == null ? System.currentTimeMillis() + CACHE_TTL_MILLIS : expiresAt;
        blacklist.put(token, expireAt);
    }

    private boolean isBlacklisted(ConcurrentHashMap<String, Long> blacklist, String token) {
        if (token == null || token.isBlank()) return false;
        Long expiresAt = blacklist.get(token);
        if (expiresAt == null) return false;
        if (expiresAt <= System.currentTimeMillis()) {
            blacklist.remove(token);
            return false;
        }
        return true;
    }

    private boolean isCacheValid(CachedSession cached) {
        if (cached == null) return false;
        if (cached.expiresAt() <= System.currentTimeMillis()) {
            return false;
        }
        SessionEntity session = cached.session();
        return session.getExpiresAt() != null && session.getExpiresAt() > System.currentTimeMillis();
    }

    private record CachedSession(SessionEntity session, long expiresAt) {}
}
