package com.zhihuiji.backend.api.common;

/** Signals an optimistic-concurrency or idempotency conflict for admin writes. */
public class AdminConflictException extends RuntimeException {
    public AdminConflictException(String message) {
        super(message);
    }
}
