package com.dxc.dxc_platform.shared.util;

import java.text.Normalizer;

/**
 * Utilitaire de normalisation des noms de rôles depuis la DB.
 *
 * "Membre equipe"       → "MEMBRE_EQUIPE"
 * "Chef d'équipe"       → "CHEF_D_EQUIPE"
 * "Responsable contrat" → "RESPONSABLE_CONTRAT"
 * "ADMIN"               → "ADMIN"
 * "Manager"             → "MANAGER"
 */
public final class RoleNormalizer {

    private RoleNormalizer() {}

    public static String normalize(String nom) {
        if (nom == null || nom.isBlank()) return "";

        return Normalizer.normalize(nom, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replaceAll("[''`]", "")
                .toUpperCase()
                .replaceAll("[\\s\\-]+", "_")
                .replaceAll("[^A-Z0-9_]", "");
    }

    public static String toSpringRole(String nom) {
        return "ROLE_" + normalize(nom);
    }
}
