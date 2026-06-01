package com.dxc.dxc_platform.service;

import com.dxc.dxc_platform.dto.MemberWorkloadDto;
import com.dxc.dxc_platform.dto.TeamRecommendationResult;
import com.dxc.dxc_platform.entity.Project;
import com.dxc.dxc_platform.enums.RiskLevel;
import com.dxc.dxc_platform.repository.ProjectRepository;
import com.dxc.dxc_platform.shared.exception.BusinessException;
import com.dxc.dxc_platform.shared.exception.NotFoundException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TeamRecommendationService {

    private final NvidiaAiClient aiClient;
    private final WorkloadAnalysisService workloadAnalysisService;
    private final ProjectRepository projectRepository;
    private final EmailService emailService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TeamRecommendationService(NvidiaAiClient aiClient,
                                     WorkloadAnalysisService workloadAnalysisService,
                                     ProjectRepository projectRepository,
                                     EmailService emailService) {
        this.aiClient = aiClient;
        this.workloadAnalysisService = workloadAnalysisService;
        this.projectRepository = projectRepository;
        this.emailService = emailService;
    }

    public TeamRecommendationResult recommendForDelayedProject(Long projectId) {
        Project project = projectRepository.findByIdAndDeletedFalse(projectId)
                .orElseThrow(() -> new NotFoundException(
                        "PROJECT_NOT_FOUND", "Projet introuvable"));

        if (project.getRiskLevel() != RiskLevel.ELEVE) {
            throw new BusinessException("RISK_NOT_HIGH_ENOUGH",
                    "Les recommandations sont uniquement disponibles pour les projets à risque ÉLEVÉ");
        }

        List<MemberWorkloadDto> workloads =
                workloadAnalysisService.analyzeTeamWorkload(project);

        if (workloads.isEmpty()) {
            return new TeamRecommendationResult(
                    project.getId(), project.getName(),
                    List.of(), "Aucun membre d'équipe trouvé.");
        }

        String prompt     = buildPrompt(project, workloads);
        String aiResponse = aiClient.chat(prompt);
        TeamRecommendationResult result = parseResponse(project, workloads, aiResponse);

        notifyChefProjet(project, result);
        return result;
    }

    // ── prompt ────────────────────────────────────────────────────────────
    private String buildPrompt(Project project, List<MemberWorkloadDto> workloads) {
        StringBuilder members = new StringBuilder();
        for (MemberWorkloadDto m : workloads) {
            members.append(String.format(
                    "- id:%d | %s | actives:%d | retard:%d | à_faire:%d | total:%d\n",
                    m.getUserId(), m.getFullName(),
                    m.getActiveTasks(), m.getOverdueTasks(),
                    m.getTodoTasks(), m.getTotalTasks()));
        }

        return """
                Tu es un assistant de gestion de projet.
                Le projet suivant est en retard critique.
                Recommande exactement 3 membres disponibles pour renforcer l'équipe.
                Réponds UNIQUEMENT en JSON valide, sans texte ni markdown autour :
                {
                  "recommendations": [
                    {"userId": 1, "reason": "justification courte en français"},
                    {"userId": 2, "reason": "justification courte en français"},
                    {"userId": 3, "reason": "justification courte en français"}
                  ],
                  "summary": "une phrase sur la stratégie de renforcement"
                }

                Projet      : %s
                Échéance    : %s
                Progression : %d%%
                Score risque: %.0f%%

                Charge des membres :
                %s

                Règle : recommande les membres avec le moins de tâches actives et aucun retard.
                """.formatted(
                project.getName(),
                project.getEndDate(),
                project.getProgressPercentage() != null ? project.getProgressPercentage() : 0,
                project.getRiskScore() != null ? project.getRiskScore() * 100 : 0,
                members);
    }

    // ── parse ─────────────────────────────────────────────────────────────
    private TeamRecommendationResult parseResponse(Project project,
                                                   List<MemberWorkloadDto> workloads,
                                                   String raw) {
        try {
            String clean = raw.replaceAll("(?s)```json|```", "").trim();
            int start = clean.indexOf('{');
            int end   = clean.lastIndexOf('}');
            if (start >= 0 && end > start) {
                clean = clean.substring(start, end + 1);
            }

            JsonNode root    = objectMapper.readTree(clean);
            String   summary = root.path("summary").asText("Renforcement recommandé.");
            JsonNode recs    = root.path("recommendations");

            List<TeamRecommendationResult.RecommendedMember> members = new ArrayList<>();

            if (recs.isArray()) {
                for (JsonNode rec : recs) {
                    long   uid    = rec.path("userId").asLong(-1);
                    String reason = rec.path("reason").asText("");
                    workloads.stream()
                            .filter(m -> m.getUserId() == uid)
                            .findFirst()
                            .ifPresent(m -> members.add(
                                    new TeamRecommendationResult.RecommendedMember(
                                            m.getUserId(), m.getFullName(),
                                            m.getEmail(), m.getActiveTasks(), reason)));
                }
            }

            // Fallback si l'IA ne retourne rien d'exploitable
            if (members.isEmpty()) {
                workloads.stream().limit(3).forEach(m -> members.add(
                        new TeamRecommendationResult.RecommendedMember(
                                m.getUserId(), m.getFullName(), m.getEmail(),
                                m.getActiveTasks(), "Membre disponible")));
            }

            return new TeamRecommendationResult(
                    project.getId(), project.getName(), members, summary);

        } catch (Exception e) {
            List<TeamRecommendationResult.RecommendedMember> fallback = new ArrayList<>();
            workloads.stream().limit(3).forEach(m -> fallback.add(
                    new TeamRecommendationResult.RecommendedMember(
                            m.getUserId(), m.getFullName(), m.getEmail(),
                            m.getActiveTasks(), "Membre disponible")));
            return new TeamRecommendationResult(
                    project.getId(), project.getName(), fallback, "Analyse indisponible");
        }
    }

    // ── email ─────────────────────────────────────────────────────────────
    private void notifyChefProjet(Project project, TeamRecommendationResult result) {
        if (project.getChefProjet() == null
                || project.getChefProjet().getEmail() == null) return;

        StringBuilder body = new StringBuilder();
        body.append("Bonjour ").append(project.getChefProjet().getPrenom())
                .append(",\n\n");
        body.append("Le projet \"").append(project.getName())
                .append("\" est en retard critique.\n");
        body.append("Membres recommandés pour renforcer l'équipe :\n\n");

        List<TeamRecommendationResult.RecommendedMember> members =
                result.getRecommendedMembers();
        for (int i = 0; i < members.size(); i++) {
            var m = members.get(i);
            body.append(i + 1).append(". ").append(m.getFullName())
                    .append(" (").append(m.getActiveTasks()).append(" tâches actives)\n")
                    .append("   → ").append(m.getReason()).append("\n\n");
        }
        body.append("Stratégie : ").append(result.getAiJustification())
                .append("\n\nCordialement,\nSystème DXC");

        try {
            emailService.sendSimpleEmail(
                    project.getChefProjet().getEmail(),
                    "[Recommandation Équipe] " + project.getName(),
                    body.toString());
        } catch (Exception e) {
            System.err.println("Erreur email recommandation : " + e.getMessage());
        }
    }
}