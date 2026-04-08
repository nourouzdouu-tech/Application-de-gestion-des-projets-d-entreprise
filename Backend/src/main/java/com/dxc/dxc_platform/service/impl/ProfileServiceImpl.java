package com.dxc.dxc_platform.service.impl;

import com.dxc.dxc_platform.dto.ProfileDto;
import com.dxc.dxc_platform.entity.Profile;
import com.dxc.dxc_platform.mapper.ProfileMapper;
import com.dxc.dxc_platform.repository.ProfileRepository;
import com.dxc.dxc_platform.service.ProfileService;
import com.dxc.dxc_platform.shared.exception.ConflictException;
import com.dxc.dxc_platform.shared.exception.NotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import static com.dxc.dxc_platform.shared.exception.ErrorCodes.PROFILE_ALREADY_EXISTS;
import static com.dxc.dxc_platform.shared.exception.ErrorCodes.PROFILE_NOT_FOUND;

@Service
@Transactional
public class ProfileServiceImpl implements ProfileService {

    private final ProfileRepository profileRepository;
    private final ProfileMapper profileMapper;

    public ProfileServiceImpl(ProfileRepository profileRepository,
                              ProfileMapper profileMapper) {
        this.profileRepository = profileRepository;
        this.profileMapper = profileMapper;
    }

    @Override
    public ProfileDto createProfile(ProfileDto request) {
        String libelle = request.getLibelle().trim();

        if (profileRepository.existsByLibelleIgnoreCaseAndDeletedFalse(libelle)) {
            throw new ConflictException(
                    PROFILE_ALREADY_EXISTS,
                    "Un profil avec ce libellé existe déjà"
            );
        }

        Profile profile = new Profile();
        profile.setLibelle(libelle);
        profile.setTjm(request.getTjm());
        profile.setDeleted(false);

        Profile savedProfile = profileRepository.save(profile);
        return profileMapper.toDto(savedProfile);
    }

    @Override
    public ProfileDto updateProfile(Long profileId, ProfileDto request) {
        Profile profile = findActiveProfileById(profileId);

        String newLibelle = request.getLibelle().trim();

        profileRepository.findByLibelleIgnoreCaseAndDeletedFalse(newLibelle)
                .ifPresent(existingProfile -> {
                    if (!existingProfile.getId().equals(profile.getId())) {
                        throw new ConflictException(
                                PROFILE_ALREADY_EXISTS,
                                "Un profil avec ce libellé existe déjà"
                        );
                    }
                });

        profile.setLibelle(newLibelle);
        profile.setTjm(request.getTjm());

        Profile updatedProfile = profileRepository.save(profile);
        return profileMapper.toDto(updatedProfile);
    }

    @Override
    public ProfileDto getProfileById(Long profileId) {
        Profile profile = findActiveProfileById(profileId);
        return profileMapper.toDto(profile);
    }

    @Override
    public List<ProfileDto> getAllProfiles() {
        return profileRepository.findAllByDeletedFalse()
                .stream()
                .map(profileMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public ProfileDto setDeletedStatus(Long profileId, boolean deleted) {
        Profile profile = findProfileByIdForDeleteRestore(profileId, deleted);

        profile.setDeleted(deleted);

        Profile savedProfile = profileRepository.save(profile);
        return profileMapper.toDto(savedProfile);
    }

    private Profile findActiveProfileById(Long profileId) {
        return profileRepository.findByIdAndDeletedFalse(profileId)
                .orElseThrow(() -> new NotFoundException(
                        PROFILE_NOT_FOUND,
                        "Profil introuvable"
                ));
    }

    private Profile findProfileByIdForDeleteRestore(Long profileId, boolean deleted) {
        if (deleted) {
            return profileRepository.findByIdAndDeletedFalse(profileId)
                    .orElseThrow(() -> new NotFoundException(
                            PROFILE_NOT_FOUND,
                            "Profil introuvable"
                    ));
        }

        return profileRepository.findById(profileId)
                .orElseThrow(() -> new NotFoundException(
                        PROFILE_NOT_FOUND,
                        "Profil introuvable"
                ));
    }
}