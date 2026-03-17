package com.dxc.dxc_platform.service;

import com.dxc.dxc_platform.dto.AuthDto;

public interface AuthService {

    AuthDto.Response login(AuthDto.LoginRequest request);

    void changePassword(String email, AuthDto.ChangePasswordRequest request);
}