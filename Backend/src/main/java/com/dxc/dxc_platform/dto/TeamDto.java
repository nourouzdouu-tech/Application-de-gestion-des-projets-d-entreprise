package com.dxc.dxc_platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TeamDto {

    private Long id;

    @NotBlank(message = "Le nom de l'équipe est obligatoire")
    @Size(max = 100, message = "Le nom de l'équipe ne doit pas dépasser 100 caractères")
    private String name;

    @Size(max = 255, message = "La description ne doit pas dépasser 255 caractères")
    private String description;

    private Long projectManagerId;
    private String projectManagerName;

    private List<MemberInfo> members = new ArrayList<>();

    private Boolean deleted;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public TeamDto() {
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Long getProjectManagerId() {
        return projectManagerId;
    }

    public String getProjectManagerName() {
        return projectManagerName;
    }

    public List<MemberInfo> getMembers() {
        return members;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setProjectManagerId(Long projectManagerId) {
        this.projectManagerId = projectManagerId;
    }

    public void setProjectManagerName(String projectManagerName) {
        this.projectManagerName = projectManagerName;
    }

    public void setMembers(List<MemberInfo> members) {
        this.members = members;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static class MemberInfo {

        private Long id;
        private String fullName;
        private String email;
        private Boolean enabled;
        private String roleName;

        private Long profileId;
        private String profileLibelle;
        private BigDecimal tjm;

        public MemberInfo() {
        }

        public MemberInfo(
                Long id,
                String fullName,
                String email,
                Boolean enabled,
                String roleName,
                Long profileId,
                String profileLibelle,
                BigDecimal tjm
        ) {
            this.id = id;
            this.fullName = fullName;
            this.email = email;
            this.enabled = enabled;
            this.roleName = roleName;
            this.profileId = profileId;
            this.profileLibelle = profileLibelle;
            this.tjm = tjm;
        }

        public Long getId() {
            return id;
        }

        public String getFullName() {
            return fullName;
        }

        public String getEmail() {
            return email;
        }

        public Boolean getEnabled() {
            return enabled;
        }

        public String getRoleName() {
            return roleName;
        }

        public Long getProfileId() {
            return profileId;
        }

        public String getProfileLibelle() {
            return profileLibelle;
        }

        public BigDecimal getTjm() {
            return tjm;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public void setRoleName(String roleName) {
            this.roleName = roleName;
        }

        public void setProfileId(Long profileId) {
            this.profileId = profileId;
        }

        public void setProfileLibelle(String profileLibelle) {
            this.profileLibelle = profileLibelle;
        }

        public void setTjm(BigDecimal tjm) {
            this.tjm = tjm;
        }
    }
}