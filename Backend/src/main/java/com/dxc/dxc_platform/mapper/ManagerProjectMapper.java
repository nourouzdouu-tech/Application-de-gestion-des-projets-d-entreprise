package com.dxc.dxc_platform.mapper;

import com.dxc.dxc_platform.dto.ManagerProjectItemDto;
import com.dxc.dxc_platform.entity.Project;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ManagerProjectMapper {

    @Mapping(source = "id", target = "id")
    @Mapping(source = "name", target = "projectName")
    @Mapping(source = "status", target = "status")
    @Mapping(source = "client", target = "client")
    @Mapping(source = "managerComment", target = "managerComment")
    @Mapping(source = "createdAt", target = "createdAt")
    @Mapping(source = "reviewedAt", target = "reviewedAt")

    @Mapping(expression = "java(getManagerFullName(project))", target = "managerName")
    @Mapping(expression = "java(getChefProjetId(project))", target = "chefProjetId")
    @Mapping(expression = "java(getChefProjetFullName(project))", target = "chefProjetName")
    ManagerProjectItemDto toDto(Project project);

    default String getManagerFullName(Project project) {
        if (project.getManager() == null) {
            return null;
        }

        String prenom = project.getManager().getPrenom() != null ? project.getManager().getPrenom() : "";
        String nom = project.getManager().getNom() != null ? project.getManager().getNom() : "";

        return (prenom + " " + nom).trim();
    }

    default Long getChefProjetId(Project project) {
        if (project.getTeam() == null || project.getTeam().getProjectManager() == null) {
            return null;
        }
        return project.getTeam().getProjectManager().getId();
    }

    default String getChefProjetFullName(Project project) {
        if (project.getTeam() == null || project.getTeam().getProjectManager() == null) {
            return null;
        }

        String prenom = project.getTeam().getProjectManager().getPrenom() != null
                ? project.getTeam().getProjectManager().getPrenom()
                : "";
        String nom = project.getTeam().getProjectManager().getNom() != null
                ? project.getTeam().getProjectManager().getNom()
                : "";

        return (prenom + " " + nom).trim();
    }
}