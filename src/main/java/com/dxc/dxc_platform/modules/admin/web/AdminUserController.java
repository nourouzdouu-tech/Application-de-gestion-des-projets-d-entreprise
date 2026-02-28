package com.dxc.dxc_platform.modules.admin.web;

import com.dxc.dxc_platform.modules.admin.domain.entity.User;
import com.dxc.dxc_platform.modules.admin.service.UserAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserAdminService userAdminService;

    @PostMapping
    public User createUser(@RequestBody CreateUserRequest request) {
        return userAdminService.createUser(
                request.getNom(),
                request.getPrenom(),
                request.getEmail(),
                request.getPassword(),
                request.getGenre(),
                request.getRoleIds()
        );
    }

    @GetMapping
    public List<User> getAllUsers() {
        return userAdminService.getAllUsers();
    }

    @GetMapping("/{id}")
    public User getUserById(@PathVariable Long id) {
        return userAdminService.getUserById(id);
    }

    @PutMapping("/{id}/unlock")
    public void unlockUser(@PathVariable Long id, @RequestParam String newPassword) {
        userAdminService.unlockUser(id, newPassword);
    }

    @DeleteMapping("/{id}")
    public void disableUser(@PathVariable Long id) {
        userAdminService.disableUser(id);
    }

    // DTO pour la création utilisateur
    public static class CreateUserRequest {
        private String nom;
        private String prenom;
        private String email;
        private String password;
        private String genre;
        private Set<Long> roleIds;

        // getters et setters
        public String getNom() { return nom; }
        public void setNom(String nom) { this.nom = nom; }
        public String getPrenom() { return prenom; }
        public void setPrenom(String prenom) { this.prenom = prenom; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getGenre() { return genre; }
        public void setGenre(String genre) { this.genre = genre; }
        public Set<Long> getRoleIds() { return roleIds; }
        public void setRoleIds(Set<Long> roleIds) { this.roleIds = roleIds; }
    }
}