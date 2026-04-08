package com.dxc.dxc_platform.controller;

import com.dxc.dxc_platform.dto.ProfileDto;
import com.dxc.dxc_platform.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/profiles")
@PreAuthorize("hasAuthority('ADMIN')")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @PostMapping
    public ResponseEntity<ProfileDto> createProfile(@Valid @RequestBody ProfileDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(profileService.createProfile(request));
    }

    @PutMapping("/{profileId}")
    public ResponseEntity<ProfileDto> updateProfile(@PathVariable Long profileId,
                                                    @Valid @RequestBody ProfileDto request) {
        return ResponseEntity.ok(profileService.updateProfile(profileId, request));
    }

    @GetMapping("/{profileId}")
    public ResponseEntity<ProfileDto> getProfileById(@PathVariable Long profileId) {
        return ResponseEntity.ok(profileService.getProfileById(profileId));
    }

    @GetMapping
    public ResponseEntity<List<ProfileDto>> getAllProfiles() {
        return ResponseEntity.ok(profileService.getAllProfiles());
    }

    @PatchMapping("/{profileId}/deleted")
    public ResponseEntity<ProfileDto> setDeletedStatus(@PathVariable Long profileId,
                                                       @RequestParam boolean deleted) {
        return ResponseEntity.ok(profileService.setDeletedStatus(profileId, deleted));
    }
}