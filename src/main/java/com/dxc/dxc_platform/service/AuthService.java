package com.dxc.dxc_platform.service;

import com.dxc.dxc_platform.dto.AuthResponse;
import com.dxc.dxc_platform.dto.ChangePasswordRequest;
import com.dxc.dxc_platform.dto.LoginRequest;

public interface AuthService {
    AuthResponse login(LoginRequest request);
    void changePassword(String email, ChangePasswordRequest request);
}
