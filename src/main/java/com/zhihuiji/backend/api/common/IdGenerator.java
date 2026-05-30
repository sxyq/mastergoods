package com.zhihuiji.backend.api.common;

import java.security.SecureRandom;
import java.util.function.BooleanSupplier;

public final class IdGenerator {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int MAX_COLLISION_RETRIES = 3;

    private IdGenerator() {}

    public static long nextId() {
        long id = SECURE_RANDOM.nextLong() & Long.MAX_VALUE;
        return id == 0L ? (System.nanoTime() & Long.MAX_VALUE) : id;
    }

    public static long nextIdWithCollisionCheck(BooleanSupplier existsCheck) {
        for (int attempt = 0; attempt < MAX_COLLISION_RETRIES; attempt++) {
            long id = nextId();
            if (!existsCheck.getAsBoolean()) {
                return id;
            }
        }
        return nextId();
    }
}
