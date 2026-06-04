package com.resumeanalyzer.service;

import com.resumeanalyzer.dto.response.UserProfileResponse;
import com.resumeanalyzer.exception.ResourceNotFoundException;
import com.resumeanalyzer.model.User;
import com.resumeanalyzer.repository.UserRepository;
import com.resumeanalyzer.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    // Get the currently logged-in user as a User model object
    public User getCurrentUser() {
        String email = SecurityUtil.getCurrentUserEmail();
        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found: " + email));
    }

    // Get user by ID (admin use)
    public User getUserById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found: " + userId));
    }

    // Get all users (admin only)
    public List<UserProfileResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::mapToProfileResponse)
                .toList();
    }

    // Map User model → safe response (no password exposed)
    public UserProfileResponse mapToProfileResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }
}