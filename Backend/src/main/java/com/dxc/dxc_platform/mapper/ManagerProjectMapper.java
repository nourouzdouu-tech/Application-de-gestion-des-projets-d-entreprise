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
    @Mapping(source = "createdAt", target = "createdAt")
    @Mapping(expression = "java(getManagerFullName(project))", target = "managerName")
    ManagerProjectItemDto toDto(Project project);

    default String getManagerFullName(Project project) {
        if (project.getManager() == null) {
            return null;
        }

        String prenom = project.getManager().getPrenom() != null ? project.getManager().getPrenom() : "";
        String nom = project.getManager().getNom() != null ? project.getManager().getNom() : "";

        return (prenom + " " + nom).trim();
    }
}