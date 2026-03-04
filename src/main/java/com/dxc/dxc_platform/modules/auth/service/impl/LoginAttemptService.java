package com.dxc.dxc_platform.modules.auth.service.impl;

import com.dxc.dxc_platform.modules.admin.domain.entity.User;
import com.dxc.dxc_platform.modules.admin.repository.UserRepository;
import com.dxc.dxc_platform.shared.exception.NotFoundException;
import org.springframework.security.authentication.LockedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginAttemptService {

    private static final int MAX_FAILED_ATTEMPTS = 3;

    private final UserRepository userRepository;

    public LoginAttemptService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Incrémente les tentatives échouées dans une transaction SÉPARÉE
     * pour que le save() soit commité même si une exception est lancée après.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registerFailedAttempt(String email) {
        User user = userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "Utilisateur introuvable"));

        int attempts = user.getFailedAttempts() + 1;
        user.setFailedAttempts(attempts);

        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setLocked(true);
            user.setEnabled(false);
            userRepository.saveAndFlush(user);
            throw new LockedException(
                    "Compte verrouillé après " + MAX_FAILED_ATTEMPTS
                            + " tentatives échouées. Contactez l'administrateur.");
        }

        userRepository.saveAndFlush(user);
    }

    /**
     * Remet les tentatives à zéro après un login réussi.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void resetAttempts(String email) {
        userRepository.findByEmailAndDeletedFalse(email).ifPresent(user -> {
            user.setFailedAttempts(0);
            userRepository.saveAndFlush(user);
        });
    }

    public int getRemainingAttempts(String email) {
        return userRepository.findByEmailAndDeletedFalse(email)
                .map(u -> MAX_FAILED_ATTEMPTS - u.getFailedAttempts())
                .orElse(0);
    }
}