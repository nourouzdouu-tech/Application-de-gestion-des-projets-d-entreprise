package com.dxc.dxc_platform.shared.exception;

public class ForbiddenException extends BusinessException {

    public ForbiddenException(String code, String message) {
        super(code, message);
    }
}