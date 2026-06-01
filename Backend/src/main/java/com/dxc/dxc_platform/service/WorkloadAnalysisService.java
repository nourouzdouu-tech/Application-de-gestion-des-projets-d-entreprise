package com.dxc.dxc_platform.service;

import com.dxc.dxc_platform.dto.MemberWorkloadDto;
import com.dxc.dxc_platform.entity.Project;
import com.dxc.dxc_platform.entity.Task;
import com.dxc.dxc_platform.entity.User;
import com.dxc.dxc_platform.enums.Status;
import com.dxc.dxc_platform.repository.TaskRepository;
import com.dxc.dxc_platform.repository.TeamRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class WorkloadAnalysisService {

    private final TaskRepository taskRepository;
    private final TeamRepository teamRepository;

    public WorkloadAnalysisService(TaskRepository taskRepository,
                                   TeamRepository teamRepository) {
        this.taskRepository = taskRepository;
        this.teamRepository = teamRepository;
    }

    public List<MemberWorkloadDto> analyzeTeamWorkload(Project project) {
        if (project.getTeam() == null) {
            return List.of();
        }

        List<User> members =
                teamRepository.findMembersByTeamId(project.getTeam().getId());

        if (members.isEmpty()) {
            return List.of();
        }

        List<MemberWorkloadDto> workloads = new ArrayList<>();

        for (User member : members) {
            List<Task> tasks = taskRepository
                    .findByAssignedToIdAndProjectIdAndDeletedFalse(
                            member.getId(), project.getId());

            int activeTasks = (int) tasks.stream()
                    .filter(t -> t.getStatus() == Status.En_cours)
                    .count();

            int overdueTasks = (int) tasks.stream()
                    .filter(t -> t.getEstimatedEndDate() != null
                            && t.getEstimatedEndDate().isBefore(LocalDate.now())
                            && t.getStatus() != Status.Terminé)
                    .count();

            int todoTasks = (int) tasks.stream()
                    .filter(t -> t.getStatus() == Status.A_faire)
                    .count();

            workloads.add(new MemberWorkloadDto(
                    member.getId(),
                    member.getPrenom() + " " + member.getNom(),
                    member.getEmail(),
                    activeTasks,
                    overdueTasks,
                    todoTasks,
                    tasks.size()
            ));
        }

        // Trier par charge croissante
        workloads.sort((a, b) -> {
            int sA = a.getActiveTasks() * 2 + a.getOverdueTasks() * 3 + a.getTodoTasks();
            int sB = b.getActiveTasks() * 2 + b.getOverdueTasks() * 3 + b.getTodoTasks();
            return Integer.compare(sA, sB);
        });

        return workloads;
    }
}