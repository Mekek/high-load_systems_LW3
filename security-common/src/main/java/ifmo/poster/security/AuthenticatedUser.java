package ifmo.poster.security;

import ifmo.poster.contracts.enums.Role;

public record AuthenticatedUser(Long id, String userName, Role role) {

    public boolean isSupervisor() {
        return role == Role.SUPERVISOR;
    }

    /** Создание/правка каталога событий, тегов и типов билетов. */
    public boolean canManageCatalog() {
        return role == Role.SUPERVISOR || role == Role.ADMIN;
    }

    /** Модерация статуса события (ACTIVE / BLOCKED). */
    public boolean canModerate() {
        return role == Role.SUPERVISOR || role == Role.CENSOR;
    }

    public boolean owns(Long userId) {
        return id != null && id.equals(userId);
    }
}
