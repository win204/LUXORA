package com.luxora.commerce.user.service;

import com.luxora.commerce.auth.security.AuthenticatedUser;
import com.luxora.commerce.auth.service.AuthService;
import com.luxora.commerce.common.exception.BadRequestException;
import com.luxora.commerce.common.exception.NotFoundException;
import com.luxora.commerce.common.exception.UnauthorizedException;
import com.luxora.commerce.user.dto.ChangePasswordRequest;
import com.luxora.commerce.user.dto.CurrentUserResponse;
import com.luxora.commerce.user.dto.PasswordChangedResponse;
import com.luxora.commerce.user.dto.UpdateProfileRequest;
import com.luxora.commerce.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    public CurrentUserService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthService authService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse findCurrentUser(AuthenticatedUser authenticatedUser) {
        return userRepository.findByEmail(authenticatedUser.email())
                .map(UserMapper::toCurrentUser)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
    }

    @Transactional
    public CurrentUserResponse updateCurrentUser(AuthenticatedUser authenticatedUser, UpdateProfileRequest request) {
        var user = userRepository.findByEmail(authenticatedUser.email())
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
        user.updateProfile(request.firstName().trim(), request.lastName().trim());
        return UserMapper.toCurrentUser(user);
    }

    @Transactional
    public PasswordChangedResponse changePassword(AuthenticatedUser authenticatedUser, ChangePasswordRequest request) {
        var user = userRepository.findByEmail(authenticatedUser.email())
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("INVALID_CURRENT_PASSWORD", "Current password is incorrect");
        }
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new BadRequestException("PASSWORD_CONFIRMATION_MISMATCH", "New password and confirmation do not match");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new BadRequestException("PASSWORD_UNCHANGED", "New password must be different from the current password");
        }

        user.changePasswordHash(passwordEncoder.encode(request.newPassword()));
        authService.revokeAllRefreshTokensForUser(user);
        return new PasswordChangedResponse("Password changed successfully");
    }
}
