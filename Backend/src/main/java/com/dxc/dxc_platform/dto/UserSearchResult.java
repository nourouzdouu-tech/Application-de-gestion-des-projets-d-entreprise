package com.dxc.dxc_platform.dto;

public class UserSearchResult {

    private Long id;
    private String fullName;
    private String email;
    private String roleName;
    private boolean alreadyInTeam;
    private String teamName;

    // Constructeur par défaut
    public UserSearchResult() {}

    // Constructeur avec 4 paramètres
    public UserSearchResult(Long id, String fullName, String email, String roleName) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.roleName = roleName;
        this.alreadyInTeam = false;
        this.teamName = null;
    }

    // ✅ NOUVEAU CONSTRUCTEUR avec 6 paramètres
    public UserSearchResult(Long id, String fullName, String email, String roleName, boolean alreadyInTeam, String teamName) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.roleName = roleName;
        this.alreadyInTeam = alreadyInTeam;
        this.teamName = teamName;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getRoleName() {
        return roleName;
    }

    public boolean isAlreadyInTeam() {
        return alreadyInTeam;
    }

    public String getTeamName() {
        return teamName;
    }

    // Setters
    public void setId(Long id) {
        this.id = id;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public void setAlreadyInTeam(boolean alreadyInTeam) {
        this.alreadyInTeam = alreadyInTeam;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }
}