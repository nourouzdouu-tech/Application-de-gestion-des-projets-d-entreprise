package com.dxc.dxc_platform.modules.admin.web.dto.user;

public record ResetPasswordResponse(
        Long userId,
        String tempPassword,
        boolean mustChangePassword
) {}