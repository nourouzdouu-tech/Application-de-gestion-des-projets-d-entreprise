package com.dxc.dxc_platform.dto;

import java.util.List;

public class TeamRecommendationResult {

    private Long projectId;
    private String projectName;
    private List<RecommendedMember> recommendedMembers;
    private String aiJustification;

    public TeamRecommendationResult() {}

    public TeamRecommendationResult(Long projectId, String projectName,
                                    List<RecommendedMember> recommendedMembers,
                                    String aiJustification) {
        this.projectId = projectId;
        this.projectName = projectName;
        this.recommendedMembers = recommendedMembers;
        this.aiJustification = aiJustification;
    }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public List<RecommendedMember> getRecommendedMembers() { return recommendedMembers; }
    public void setRecommendedMembers(List<RecommendedMember> r) { this.recommendedMembers = r; }

    public String getAiJustification() { return aiJustification; }
    public void setAiJustification(String j) { this.aiJustification = j; }

    // ── classe interne ──────────────────────────────────────────────────────
    public static class RecommendedMember {

        private Long userId;
        private String fullName;
        private String email;
        private int activeTasks;
        private String reason;

        public RecommendedMember() {}

        public RecommendedMember(Long userId, String fullName, String email,
                                 int activeTasks, String reason) {
            this.userId = userId;
            this.fullName = fullName;
            this.email = email;
            this.activeTasks = activeTasks;
            this.reason = reason;
        }

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public int getActiveTasks() { return activeTasks; }
        public void setActiveTasks(int activeTasks) { this.activeTasks = activeTasks; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}