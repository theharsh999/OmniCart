package com.omnicart.controller;

import com.omnicart.dto.Dtos.ProfileUpdateRequest;
import com.omnicart.dto.Dtos.UserResponse;
import com.omnicart.entity.User;
import com.omnicart.exception.ApiException;
import com.omnicart.repository.UserRepository;
import com.omnicart.util.Mapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserRepository userRepository;

    @GetMapping
    @Transactional(readOnly = true)
    public UserResponse getProfile(Authentication auth) {
        User user = getCurrentUser(auth);
        return Mapper.toUser(user);
    }

    @PutMapping
    @Transactional
    public UserResponse updateProfile(@Valid @RequestBody ProfileUpdateRequest req, Authentication auth) {
        User user = getCurrentUser(auth);

        // Editable fields: name, phone, avatar
        user.setName(req.name().trim());
        if (req.phone() != null) {
            user.setPhone(req.phone().trim());
        }
        if (req.avatar() != null) {
            if (req.avatar().length() > 3_500_000) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Profile image is too large (max 2MB)");
            }
            user.setAvatar(req.avatar().trim().isEmpty() ? null : req.avatar().trim());
        }

        // Email, role, employeeId, passwordHash cannot be altered through profile endpoint
        User saved = userRepository.save(user);
        return Mapper.toUser(saved);
    }

    private User getCurrentUser(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Please log in to continue");
        }
        return userRepository.findByEmailIgnoreCase(auth.getName())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User account not found"));
    }
}
