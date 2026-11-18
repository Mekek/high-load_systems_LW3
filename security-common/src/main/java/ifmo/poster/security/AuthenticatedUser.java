package ifmo.poster.security;

import ifmo.poster.contracts.enums.Role;

public record AuthenticatedUser(Long id, String userName, Role role) {

    public boolean isSupervisor() {
        return role == Role.SUPERVISOR;
    }

    public boolean owns(Long userId) {
        return id != null && id.equals(userId);
    }
}
