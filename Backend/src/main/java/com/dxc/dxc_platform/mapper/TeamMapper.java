package com.dxc.dxc_platform.mapper;

import com.dxc.dxc_platform.dto.MemberResponse;
import com.dxc.dxc_platform.dto.TeamResponse;
import com.dxc.dxc_platform.entity.Role;
import com.dxc.dxc_platform.entity.Team;
import com.dxc.dxc_platform.entity.User;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class TeamMapper {

    public TeamResponse toResponse(Team team) {
        TeamResponse response = new TeamResponse();
        response.setId(team.getId());
        response.setName(team.getName());
        response.setDescription(team.getDescription());
        response.setCreatedAt(team.getCreatedAt());
        response.setUpdatedAt(team.getUpdatedAt());

        if (team.getProjectManager() != null) {
            response.setProjectManagerId(team.getProjectManager().getId());
            response.setProjectManagerName(buildFullName(team.getProjectManager()));
        }

        List<MemberResponse> members = team.getMembers() == null
                ? Collections.emptyList()
                : team.getMembers().stream()
                .map(this::toMemberResponse)
                .collect(Collectors.toList());

        response.setMembers(members);

        return response;
    }

    public MemberResponse toMemberResponse(User user) {
        MemberResponse response = new MemberResponse();
        response.setId(user.getId());
        response.setFullName(buildFullName(user));
        response.setEmail(user.getEmail());
        response.setEnabled(!user.isDeleted() && !user.isLocked());
        response.setRoleName(extractRoles(user.getRoles()));
        return response;
    }

    private String buildFullName(User user) {
        String prenom = user.getPrenom() != null ? user.getPrenom() : "";
        String nom = user.getNom() != null ? user.getNom() : "";
        return (prenom + " " + nom).trim();
    }

    private String extractRoles(Set<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            return null;
        }

        return roles.stream()
                .map(Role::getNom)
                .collect(Collectors.joining(", "));
    }
}