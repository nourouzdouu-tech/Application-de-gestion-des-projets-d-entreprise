package com.dxc.dxc_platform.controller;

import com.dxc.dxc_platform.dto.UserDto;
import com.dxc.dxc_platform.entity.User;
import com.dxc.dxc_platform.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @GetMapping("/{id}")
    public ResponseEntity<UserDto.Response> getById(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
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
                null,
                user.getProfile() != null ? user.getProfile().getId() : null,
                user.getProfile() != null ? user.getProfile().getLibelle() : null,
                user.getProfile() != null ? user.getProfile().getTjm() : null  // ← ajoute ça
        );
    }
}