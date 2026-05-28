package com.warehouse.management.common;

public final class CurrentUserContext {

    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    private CurrentUserContext() {
    }

    public static void set(CurrentUser currentUser) {
        HOLDER.set(currentUser);
    }

    public static CurrentUser getRequired() {
        CurrentUser currentUser = HOLDER.get();
        if (currentUser == null) {
            throw BusinessException.unauthorized("Login is required");
        }
        return currentUser;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
