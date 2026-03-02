package com.dxc.dxc_platform.modules.admin.service;

import com.dxc.dxc_platform.modules.admin.web.dto.user.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserAdminService {

    UserResponse create(CreateUserRequest req);

    Page<UserResponse> search(String q, String role, Boolean enabled, Pageable pageable);

    UserResponse getById(Long id);

    UserResponse update(Long id, UpdateUserRequest req);

    void disable(Long id);

    void enable(Long id);

    ResetPasswordResponse resetPassword(Long id, ResetPasswordRequest req);

    void softDelete(Long id);
}