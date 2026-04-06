package com.dxc.dxc_platform.mapper;

import com.dxc.dxc_platform.dto.ProjectDto;
import com.dxc.dxc_platform.entity.Project;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProjectMapper {

    @Mapping(source = "team.id", target = "teamId")
    @Mapping(source = "team.name", target = "teamName")

    // 🔥 AJOUT MANAGER
    @Mapping(source = "manager.id", target = "managerId")
    @Mapping(expression = "java(getManagerFullName(project))", target = "managerName")

    @Mapping(source = "deleted", target = "deleted")
    ProjectDto toDto(Project project);

    // 🔥 Méthode pour concat prénom + nom
    default String getManagerFullName(Project project) {
        if (project.getManager() == null) {
            return null;
        }

        String prenom = project.getManager().getPrenom() != null ? project.getManager().getPrenom() : "";
        String nom = project.getManager().getNom() != null ? project.getManager().getNom() : "";

        return (prenom + " " + nom).trim();
    }
}