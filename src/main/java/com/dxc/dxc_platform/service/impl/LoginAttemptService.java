package com.dxc.dxc_platform.service.impl;

import com.dxc.dxc_platform.entity.User;
import com.dxc.dxc_platform.repository.UserRepository;
import com.dxc.dxc_platform.shared.exception.NotFoundException;
import org.springframework.security.authentication.LockedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginAttemptService {

    private static final int MAX_FAILED_ATTEMPTS = 3;

    private final UserRepository userRepository;
    private final LockUserService lockUserService;

    public LoginAttemptService(UserRepository userRepository,
                               LockUserService lockUserService) {
        this.userRepository = userRepository;
        this.lockUserService = lockUserService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int registerFailedAttempt(String email) {

        User user = userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> new NotFoundException(
                        "USER_NOT_FOUND", "Utilisateur introuvable"));

        int attempts = user.getFailedAttempts() + 1;
        System.out.println(">>> TENTATIVE " + attempts + " pour " + email);

        if (attempts >= MAX_FAILED_ATTEMPTS) {
            // Verrouiller dans un bean séparé → transaction propre commitée
            // AVANT de lancer l'exception
            lockUserService.lockUser(email);
            throw new LockedException(
                    "Compte verrouillé après " + MAX_FAILED_ATTEMPTS
                            + " tentatives échouées. Contactez l'administrateur.");
        }

        userRepository.incrementFailedAttempts(email);
        System.out.println(">>> incrementFailedAttempts() TERMINÉ");
        return MAX_FAILED_ATTEMPTS - attempts;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void resetAttempts(String email) {
        userRepository.resetFailedAttempts(email);
        System.out.println(">>> resetAttempts() TERMINÉ pour " + email);
    }
}