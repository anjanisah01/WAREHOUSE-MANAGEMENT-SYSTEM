package com.enterprise.wms.dto;

import java.util.Set;

/**
 * DTOs for user management (create, update roles, response).
 */
public class UserDtos {

    /** Request to create a new user with an initial set of roles. */
    public record CreateUserRequest(String username, String password, Set<String> roles) {}

    /** Request to replace a user’s roles. */
    public record UpdateRolesRequest(Set<String> roles) {}

    /** Read-only user response returned by the API. */
    public record UserResponse(Long id, String username, Set<String> roles) {}
}
