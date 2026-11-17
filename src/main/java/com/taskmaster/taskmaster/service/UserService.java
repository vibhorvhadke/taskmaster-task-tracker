package com.taskmaster.taskmaster.service;

import com.taskmaster.taskmaster.dto.user.ChangePasswordRequest;
import com.taskmaster.taskmaster.dto.user.UpdateProfileRequest;
import com.taskmaster.taskmaster.dto.user.UserResponse;
import com.taskmaster.taskmaster.entity.User;
import com.taskmaster.taskmaster.exception.BadRequestException;
import com.taskmaster.taskmaster.exception.ResourceNotFoundException;
import com.taskmaster.taskmaster.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Profile management for the logged-in user.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserResponse getProfile(Long userId) {
        return UserResponse.from(findUser(userId));
    }

    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findUser(userId);
        user.setFullName(request.fullName().trim());

        // saveAndFlush writes the change immediately, so @UpdateTimestamp
        // sets updatedAt before we build the response.
        return UserResponse.from(userRepository.saveAndFlush(user));
    }

    /**
     * Changes the password after verifying the current one.
     *
     * @throws BadRequestException if the current password is wrong,
     *                             or the new password is the same as the current one
     */
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = findUser(userId);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new BadRequestException("New password must be different from the current password");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }
}