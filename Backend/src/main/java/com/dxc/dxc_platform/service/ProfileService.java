package com.dxc.dxc_platform.service;

import com.dxc.dxc_platform.dto.ProfileDto;

import java.util.List;

public interface ProfileService {

    ProfileDto createProfile(ProfileDto request);

    ProfileDto updateProfile(Long profileId, ProfileDto request);

    ProfileDto getProfileById(Long profileId);

    List<ProfileDto> getAllProfiles();

    ProfileDto setDeletedStatus(Long profileId, boolean deleted);
}