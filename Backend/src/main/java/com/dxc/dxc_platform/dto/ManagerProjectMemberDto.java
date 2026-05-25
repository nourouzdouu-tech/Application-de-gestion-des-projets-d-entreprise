package com.dxc.dxc_platform.dto;

public class ManagerProjectMemberDto {
    private Long id;
    private String fullName;
    private String email;
    private String profile;
    private int tjm;
    private String role;

    public ManagerProjectMemberDto() {}

    public ManagerProjectMemberDto(Long id, String fullName, String email, String profile, int tjm, String role) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.profile = profile;
        this.tjm = tjm;
        this.role = role;
    }

    // Getters
    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getProfile() { return profile; }
    public int getTjm() { return tjm; }
    public String getRole() { return role; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setEmail(String email) { this.email = email; }
    public void setProfile(String profile) { this.profile = profile; }
    public void setTjm(int tjm) { this.tjm = tjm; }
    public void setRole(String role) { this.role = role; }
}