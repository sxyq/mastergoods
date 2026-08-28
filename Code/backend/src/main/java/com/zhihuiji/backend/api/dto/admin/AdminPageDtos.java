package com.zhihuiji.backend.api.dto.admin;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;
import java.util.List;

/** Common bounded page envelope for administrator read APIs. */
public final class AdminPageDtos {
    private AdminPageDtos() {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record PageResponse<T>(
        List<T> items,
        int page,
        int size,
        long total,
        boolean hasNext,
        Instant generatedAt,
        AdminScopeDtos.Scope scope,
        String scopeCompleteness
    ) {
        public PageResponse {
            items = List.copyOf(items == null ? List.of() : items);
        }
    }
}
