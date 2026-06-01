package com.dxc.dxc_platform.service;

import com.dxc.dxc_platform.dto.RiskPredictionResult;
import com.dxc.dxc_platform.entity.Project;
import com.dxc.dxc_platform.entity.Task;
import com.dxc.dxc_platform.enums.ProjectStatus;
import com.dxc.dxc_platform.enums.RiskLevel;
import com.dxc.dxc_platform.enums.Status;
import com.dxc.dxc_platform.repository.ProjectRepository;
import com.dxc.dxc_platform.repository.TaskRepository;
import com.dxc.dxc_platform.shared.exception.BusinessException;
import com.dxc.dxc_platform.shared.exception.NotFoundException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PredictionService {

    private final NvidiaAiClient aiClient;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final RiskAlertService riskAlertService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PredictionService(NvidiaAiClient aiClient,
                             ProjectRepository projectRepository,
                             TaskRepository taskRepository,
                             RiskAlertService riskAlertService,
                             @Lazy TeamRecommendationService teamRecommendationService) {
        this.aiClient = aiClient;
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.riskAlertService = riskAlertService;
    }

    @Transactional
    public RiskPredictionResult predictForProject(Long projectId) {
        Project project = projectRepository.findByIdAndDeletedFalse(projectId)
                .orElseThrow(() -> new NotFoundException(
                        "PROJECT_NOT_FOUND", "Projet introuvable"));

        if (project.getStatus() != ProjectStatus.EN_COURS) {
            throw new BusinessException("INVALID_PROJECT_STATUS",
                    "La prédiction est uniquement disponible pour les projets EN_COURS");
        }

        // Cache valide → retourner sans appel IA
        if (isCacheValid(project)) {
            System.out.println("📦 Cache utilisé pour projet " + project.getName());
            return new RiskPredictionResult(
                    project.getId(), project.getName(),
                    project.getRiskLevel(),
                    project.getRiskScore() != null ? project.getRiskScore() : 0.0,
                    project.getRiskReason() != null ? project.getRiskReason() : "Analyse en cache"
            );
        }

        System.out.println("🤖 Appel IA pour projet " + project.getName());
        List<Task> tasks  = taskRepository.findByProjectIdAndDeletedFalse(projectId);
        String prompt     = buildPrompt(project, tasks);
        String aiResponse = aiClient.chat(prompt);
        RiskPredictionResult result = parseAiResponse(project, aiResponse);

        project.setRiskLevel(result.getLevel());
        project.setRiskScore(result.getScore());
        project.setRiskReason(result.getReason());
        project.setRiskUpdatedAt(LocalDateTime.now());
        projectRepository.save(project);

        if (result.getLevel() == RiskLevel.MOYEN || result.getLevel() == RiskLevel.ELEVE) {
            try { riskAlertService.sendRiskAlert(project, result); }
            catch (Exception e) { System.err.println("⚠️ Email: " + e.getMessage()); }
        }

        // ✅ PAS d'appel recommandation ici — sera chargé séparément depuis le cache
        return result;
    }

    private boolean isCacheValid(Project project) {
        return project.getRiskLevel() != null
                && project.getRiskUpdatedAt() != null
                && project.getRiskUpdatedAt().isAfter(LocalDateTime.now().minusHours(6));
    }

    // Force l'appel IA même si le cache est valide
    @Transactional
    public RiskPredictionResult forceRefreshForProject(Long projectId) {
        Project project = projectRepository.findByIdAndDeletedFalse(projectId)
                .orElseThrow(() -> new NotFoundException(
                        "PROJECT_NOT_FOUND", "Projet introuvable"));

        if (project.getStatus() != ProjectStatus.EN_COURS) {
            throw new BusinessException("INVALID_PROJECT_STATUS",
                    "La prédiction est uniquement disponible pour les projets EN_COURS");
        }

        List<Task> tasks  = taskRepository.findByProjectIdAndDeletedFalse(projectId);
        String prompt     = buildPrompt(project, tasks);
        String aiResponse = aiClient.chat(prompt);
        RiskPredictionResult result = parseAiResponse(project, aiResponse);

        project.setRiskLevel(result.getLevel());
        project.setRiskScore(result.getScore());
        project.setRiskReason(result.getReason());
        project.setRiskUpdatedAt(LocalDateTime.now());
        projectRepository.save(project);

        if (result.getLevel() == RiskLevel.MOYEN || result.getLevel() == RiskLevel.ELEVE) {
            try { riskAlertService.sendRiskAlert(project, result); }
            catch (Exception e) { System.err.println("⚠️ Email: " + e.getMessage()); }
        }

        return result;
    }


    

    public List<RiskPredictionResult> predictAllActiveProjects() {
        return projectRepository.findByStatus(ProjectStatus.EN_COURS)
                .stream()
                .map(p -> {
                    try {
                        return predictForProject(p.getId());
                    } catch (Exception e) {
                        return new RiskPredictionResult(
                                p.getId(), p.getName(),
                                RiskLevel.FAIBLE, 0.0, "Analyse indisponible");
                    }
                })
                .collect(Collectors.toList());
    }

    private String buildPrompt(Project project, List<Task> tasks) {
        long total      = tasks.size();
        long done       = tasks.stream()
                .filter(t -> t.getStatus() == Status.Terminé).count();
        long inProgress = tasks.stream()
                .filter(t -> t.getStatus() == Status.En_cours).count();
        long overdue    = tasks.stream()
                .filter(t -> t.getEstimatedEndDate() != null
                        && t.getEstimatedEndDate().isBefore(LocalDate.now())
                        && t.getStatus() != Status.Terminé)
                .count();
        long daysLeft = project.getEndDate() != null
                ? ChronoUnit.DAYS.between(LocalDate.now(), project.getEndDate()) : 0;
        int progress = project.getProgressPercentage() != null
                ? project.getProgressPercentage() : 0;

        return """
                You are a project risk analyst.
                Respond ONLY with valid JSON, no extra text, no markdown:
                {"level":"FAIBLE|MOYEN|ELEVE","score":0.0,"reason":"une phrase en français"}

                Project  : %s
                Deadline : %s (%d days remaining)
                Progress : %d%%
                Tasks    : total=%d | done=%d | in_progress=%d | overdue=%d

                Rules: FAIBLE < 0.35 | MOYEN 0.35-0.70 | ELEVE > 0.70
                """.formatted(
                project.getName(), project.getEndDate(),
                daysLeft, progress,
                total, done, inProgress, overdue);
    }

    private RiskPredictionResult parseAiResponse(Project project, String raw) {
        try {
            String clean = raw.replaceAll("(?s)```json|```", "").trim();
            int start = clean.indexOf('{');
            int end   = clean.lastIndexOf('}');
            if (start >= 0 && end > start) {
                clean = clean.substring(start, end + 1);
            }
            JsonNode node   = objectMapper.readTree(clean);
            RiskLevel level = RiskLevel.valueOf(
                    node.path("level").asText("FAIBLE"));
            double score    = node.path("score").asDouble(0.0);
            String reason   = node.path("reason").asText("Analyse non disponible");
            return new RiskPredictionResult(
                    project.getId(), project.getName(), level, score, reason);
        } catch (Exception e) {
            System.err.println("⚠️ Parse réponse IA échoué : " + e.getMessage());
            return new RiskPredictionResult(
                    project.getId(), project.getName(),
                    RiskLevel.FAIBLE, 0.0, "Analyse indisponible");
        }
    }
}