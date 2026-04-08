package com.dxc.dxc_platform.repository;

import com.dxc.dxc_platform.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProfileRepository extends JpaRepository<Profile, Long> {

    boolean existsByLibelleIgnoreCaseAndDeletedFalse(String libelle);

    Optional<Profile> findByIdAndDeletedFalse(Long id);

    Optional<Profile> findByLibelleIgnoreCaseAndDeletedFalse(String libelle);

    List<Profile> findAllByDeletedFalse();
}