package com.warehouse.management.util;

import java.util.function.Supplier;

public final class PaginationSupport {

    private static final long DEFAULT_PAGE_SIZE = 10L;

    private static final long DEFAULT_MAX_PAGE_SIZE = 100L;

    private static final ThreadLocal<Long> MAX_PAGE_SIZE = new ThreadLocal<>();

    private PaginationSupport() {
    }

    public static long normalizeSize(long size) {
        if (size < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, currentMaxPageSize());
    }

    public static <T> T withMaxPageSize(long maxPageSize, Supplier<T> action) {
        Long previous = MAX_PAGE_SIZE.get();
        MAX_PAGE_SIZE.set(Math.max(DEFAULT_MAX_PAGE_SIZE, maxPageSize));
        try {
            return action.get();
        } finally {
            if (previous == null) {
                MAX_PAGE_SIZE.remove();
            } else {
                MAX_PAGE_SIZE.set(previous);
            }
        }
    }

    private static long currentMaxPageSize() {
        Long configured = MAX_PAGE_SIZE.get();
        return configured == null ? DEFAULT_MAX_PAGE_SIZE : configured;
    }
}
