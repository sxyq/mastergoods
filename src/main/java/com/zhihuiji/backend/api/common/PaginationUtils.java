package com.zhihuiji.backend.api.common;

import java.util.Collections;
import java.util.List;

public final class PaginationUtils {
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 50;
    private static final int MAX_SIZE = 200;

    private PaginationUtils() {}

    public static <T> List<T> slice(List<T> source, Integer page, Integer size) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        int safePage = page == null || page < 0 ? DEFAULT_PAGE : page;
        int safeSize = size == null || size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        int sourceSize = source.size();
        int fromIndex = safePage * safeSize;
        if (fromIndex >= sourceSize) {
            return Collections.emptyList();
        }
        int toIndex = Math.min(sourceSize, fromIndex + safeSize);
        return source.subList(fromIndex, toIndex);
    }
}
