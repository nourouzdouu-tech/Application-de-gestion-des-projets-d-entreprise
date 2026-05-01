package com.dxc.dxc_platform.service;

import com.dxc.dxc_platform.dto.ManagerSelectDto;
import com.dxc.dxc_platform.dto.UserDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface UserAdminService {
    UserDto.Response create(UserDto.CreateRequest req);

    Page<UserDto.Response> search(String q, String role, Boolean locked, Pageable pageable);

    UserDto.Response getById(Long id);

    UserDto.Response update(Long id, UserDto.UpdateRequest req);

    void disable(Long id);

    void enable(Long id);

    UserDto.ResetPasswordResponse resetPassword(Long id, UserDto.ResetPasswordRequest req);

    void softDelete(Long id);

    List<ManagerSelectDto> getManagersForSelect();
}