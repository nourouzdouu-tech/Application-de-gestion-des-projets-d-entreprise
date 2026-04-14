package com.dxc.dxc_platform.controller;

import com.dxc.dxc_platform.dto.UserDto;
import com.dxc.dxc_platform.entity.User;
import com.dxc.dxc_platform.repository.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/list")
    public List<UserDto.Response> getAllUsers() {
        return userRepository.findAll().stream()
                .filter(user -> !user.isDeleted())
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private UserDto.Response toResponse(User user) {
        return new UserDto.Response(
                user.getId(),
                user.getEmail(),
                user.getPrenom(),
                user.getNom(),
                user.getGenre(),
                user.getFailedAttempts(),
                user.isLocked(),
                user.isMustChangePassword(),
                null, // vous pouvez mapper les rôles si nécessaire
                user.getProfile() != null ? user.getProfile().getId() : null,
                user.getProfile() != null ? user.getProfile().getLibelle() : null
        );
    }
}