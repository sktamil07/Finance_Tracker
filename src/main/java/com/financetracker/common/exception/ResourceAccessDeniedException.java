package com.financetracker.common.exception;

/**
 * The row exists but belongs to another user. Maps to 403.
 * <p>
 * Contrast with {@link ResourceNotFoundException} (404), which means the row does not exist for
 * anyone. Distinguishing the two leaks the existence of a foreign id, which is an accepted
 * trade-off here because the acceptance criteria require an observable 403 on ownership
 * violations. Flip {@code OwnershipGuard} to raise 404 instead if that leak matters to you.
 */
public class ResourceAccessDeniedException extends RuntimeException {

    public ResourceAccessDeniedException(String message) {
        super(message);
    }

    public static ResourceAccessDeniedException of(String resource, Object id) {
        return new ResourceAccessDeniedException("%s %s belongs to another user".formatted(resource, id));
    }
}
