package com.dxc.dxc_platform.controller;

import com.dxc.dxc_platform.dto.ManagerSelectDto;
import com.dxc.dxc_platform.dto.UserDto;
import com.dxc.dxc_platform.service.UserAdminService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserAdminService userAdminService;

    public AdminUserController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @PostMapping
    public ResponseEntity<UserDto.Response> create(@Valid @RequestBody UserDto.CreateRequest req) {
        return ResponseEntity.ok(userAdminService.create(req));
    }

    @GetMapping
    public ResponseEntity<Page<UserDto.Response>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean locked,
            @PageableDefault(size = 10) Pageable pageable,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(userAdminService.search(q, role, locked, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto.Response> get(@PathVariable Long id) {
        return ResponseEntity.ok(userAdminService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDto.Response> update(@PathVariable Long id,
                                                   @Valid @RequestBody UserDto.UpdateRequest req) {
        return ResponseEntity.ok(userAdminService.update(id, req));
    }

    @PatchMapping("/{id}/disable")
    public ResponseEntity<Void> disable(@PathVariable Long id) {
        userAdminService.disable(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/enable")
    public ResponseEntity<Void> enable(@PathVariable Long id) {
        userAdminService.enable(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<UserDto.ResetPasswordResponse> resetPassword(
            @PathVariable Long id,
            @RequestBody(required = false) UserDto.ResetPasswordRequest req) {
        return ResponseEntity.ok(userAdminService.resetPassword(id, req));
    }

    // ✅ NOUVEAU : endpoint appelé quand l'utilisateur change son mot de passe
    @PatchMapping("/{id}/change-password")
    public ResponseEntity<Void> changePassword(
            @PathVariable Long id,
            @RequestBody UserDto.ChangePasswordRequest req) {
        userAdminService.changePassword(id, req.newPassword());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDelete(@PathVariable Long id) {
        userAdminService.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}