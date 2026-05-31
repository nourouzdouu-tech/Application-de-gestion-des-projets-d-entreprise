package com.dxc.dxc_platform.mapper;

import com.dxc.dxc_platform.dto.ProjectDto;
import com.dxc.dxc_platform.entity.Project;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProjectMapper {

    @Mapping(source = "team.id", target = "teamId")
    @Mapping(source = "team.name", target = "teamName")
    @Mapping(source = "manager.id", target = "managerId")
    @Mapping(expression = "java(getManagerFullName(project))", target = "managerName")
    @Mapping(source = "managerComment", target = "managerComment")
    @Mapping(source = "reviewedAt", target = "reviewedAt")
    @Mapping(source = "deleted", target = "deleted")
    @Mapping(source = "chefProjet.id", target = "chefProjetId")
    @Mapping(expression = "java(getChefProjetFullName(project))", target = "chefProjetName")
    @Mapping(source = "riskScore", target = "riskScore")
    @Mapping(source = "riskReason", target = "riskReason")
    @Mapping(source = "riskUpdatedAt", target = "riskUpdatedAt")
    ProjectDto toDto(Project project);

    default String getManagerFullName(Project project) {
        if (project.getManager() == null) {
            return null;
        }

        String prenom = project.getManager().getPrenom() != null ? project.getManager().getPrenom() : "";
        String nom = project.getManager().getNom() != null ? project.getManager().getNom() : "";

        return (prenom + " " + nom).trim();
    }
    default String getChefProjetFullName(Project project) {
        if (project.getChefProjet() == null) return null;
        return (project.getChefProjet().getPrenom() + " " + project.getChefProjet().getNom()).trim();
    }
}