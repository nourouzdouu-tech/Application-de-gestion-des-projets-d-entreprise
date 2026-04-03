package com.dxc.dxc_platform.controller;

import com.dxc.dxc_platform.dto.ClientDto;
import com.dxc.dxc_platform.service.ClientService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/clients")
public class AdminClientController {

    private final ClientService clientService;

    public AdminClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @PostMapping
    public ResponseEntity<ClientDto.Response> create(@Valid @RequestBody ClientDto.CreateRequest req) {
        return ResponseEntity.ok(clientService.create(req));
    }

    @GetMapping
    public ResponseEntity<Page<ClientDto.Response>> search(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(clientService.search(q, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClientDto.Response> get(@PathVariable Long id) {
        return ResponseEntity.ok(clientService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClientDto.Response> update(
            @PathVariable Long id,
            @Valid @RequestBody ClientDto.UpdateRequest req) {
        return ResponseEntity.ok(clientService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDelete(@PathVariable Long id) {
        clientService.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}