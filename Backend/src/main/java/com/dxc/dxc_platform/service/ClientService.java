package com.dxc.dxc_platform.service;

import com.dxc.dxc_platform.dto.ClientDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ClientService {
    ClientDto.Response create(ClientDto.CreateRequest req);
    Page<ClientDto.Response> search(String q, Pageable pageable);
    ClientDto.Response getById(Long id);
    ClientDto.Response update(Long id, ClientDto.UpdateRequest req);
    void softDelete(Long id);
}