package com.financetracker.common.util;

import com.financetracker.common.exception.ResourceAccessDeniedException;

import java.util.Objects;

/**
 * Single choke point for the "this row is not yours" decision, so the 403-vs-404 policy lives in
 * exactly one place.
 */
public final class OwnershipGuard {

    private OwnershipGuard() {
    }

    public static void requireOwner(Long ownerId, Long currentUserId, String resource, Object id) {
        if (!Objects.equals(ownerId, currentUserId)) {
            throw ResourceAccessDeniedException.of(resource, id);
        }
    }
}
