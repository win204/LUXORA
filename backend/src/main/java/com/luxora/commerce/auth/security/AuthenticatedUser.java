package com.luxora.commerce.auth.security;

import java.util.List;
import java.util.UUID;

public record AuthenticatedUser(UUID id, String email, List<String> roles) {
}
