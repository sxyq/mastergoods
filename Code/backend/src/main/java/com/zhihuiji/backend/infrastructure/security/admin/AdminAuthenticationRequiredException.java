package com.zhihuiji.backend.infrastructure.security.admin;

import org.springframework.security.core.AuthenticationException;

/** Signals that no authenticated session exists; mapped to HTTP 401. */
public class AdminAuthenticationRequiredException extends AuthenticationException {
    public AdminAuthenticationRequiredException() {
        super("administrator authentication required");
    }
}
