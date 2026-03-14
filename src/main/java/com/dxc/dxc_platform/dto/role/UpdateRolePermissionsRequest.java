package com.dxc.dxc_platform.dto.role;

import java.util.Set;

public record UpdateRolePermissionsRequest(
        Set<String> permissionCodes
) {}