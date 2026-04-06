package com.dxc.dxc_platform.controller;

import com.dxc.dxc_platform.dto.ClientDto;
import com.dxc.dxc_platform.service.ClientService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clients")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping("/select")
    public ResponseEntity<List<ClientDto.Response>> getClientsForSelect() {
        List<ClientDto.Response> clients = clientService
                .search(null, PageRequest.of(0, 100))
                .getContent();

        return ResponseEntity.ok(clients);
    }
}