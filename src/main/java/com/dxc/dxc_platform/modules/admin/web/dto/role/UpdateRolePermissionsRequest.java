package com.dxc.dxc_platform.modules.admin.web.dto.role;

import java.util.Set;

public record UpdateRolePermissionsRequest(
        Set<String> permissionCodes
) {}