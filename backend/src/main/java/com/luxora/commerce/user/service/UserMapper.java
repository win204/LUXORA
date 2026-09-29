package com.luxora.commerce.user.service;

import com.luxora.commerce.user.dto.CurrentUserResponse;
import com.luxora.commerce.user.model.Role;
import com.luxora.commerce.user.model.User;
import java.util.Comparator;

public final class UserMapper {

    private UserMapper() {
    }

    public static CurrentUserResponse toCurrentUser(User user) {
        return new CurrentUserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.isEnabled(),
                user.getRoles().stream().map(Role::getName).sorted(Comparator.naturalOrder()).toList(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
