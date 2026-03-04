package com.dxc.dxc_platform.shared.exception;

public class UnauthorizedException extends BusinessException {

    public UnauthorizedException(String code, String message) {
        super(code, message);
    }
}