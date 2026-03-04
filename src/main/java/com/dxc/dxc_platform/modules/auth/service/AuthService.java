package com.dxc.dxc_platform.modules.auth.service;

import com.dxc.dxc_platform.modules.auth.web.dto.AuthResponse;
import com.dxc.dxc_platform.modules.auth.web.dto.ChangePasswordRequest;
import com.dxc.dxc_platform.modules.auth.web.dto.LoginRequest;

public interface AuthService {
    AuthResponse login(LoginRequest request);
    void changePassword(String email, ChangePasswordRequest request);
}
