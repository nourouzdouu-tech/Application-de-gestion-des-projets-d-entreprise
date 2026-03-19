package com.dxc.dxc_platform.service.impl;

import com.dxc.dxc_platform.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LockUserService {

    private final UserRepository userRepository;

    public LockUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Verrouille le compte dans une transaction INDÉPENDANTE.
     * Cette méthode commit immédiatement AVANT que LockedException
     * soit lancée dans LoginAttemptService.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void lockUser(String email) {
        System.out.println(">>> LockUserService.lockUser() pour " + email);
        userRepository.lockUser(email);
        System.out.println(">>> lockUser() COMMITÉ pour " + email);
    }
}