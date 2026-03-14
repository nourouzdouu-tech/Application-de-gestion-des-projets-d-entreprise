package com.dxc.dxc_platform.dto.user;

public record ResetPasswordResponse(
        Long userId,
        String tempPassword,
        boolean mustChangePassword
) {}