package com.zhihuiji.backend.infrastructure.security;

import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class TokenService {
    public String issueToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}

