package com.dxc.dxc_platform.dto;

public class MemberResponse {

    private Long id;
    private String fullName;
    private String email;
    private Boolean enabled;
    private String roleName;

    public MemberResponse() {
    }

    public MemberResponse(Long id, String fullName, String email, Boolean enabled, String roleName) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.enabled = enabled;
        this.roleName = roleName;
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
}