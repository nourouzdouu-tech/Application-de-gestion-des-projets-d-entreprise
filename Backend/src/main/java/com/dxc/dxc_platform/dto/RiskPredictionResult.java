package com.dxc.dxc_platform.dto;

import com.dxc.dxc_platform.enums.RiskLevel;

public class RiskPredictionResult {

    private Long projectId;
    private String projectName;
    private RiskLevel level;
    private double score;
    private String reason;

    public RiskPredictionResult() {}

    public RiskPredictionResult(Long projectId, String projectName,
                                RiskLevel level, double score, String reason) {
        this.projectId = projectId;
        this.projectName = projectName;
        this.level = level;
        this.score = score;
        this.reason = reason;
    }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public RiskLevel getLevel() { return level; }
    public void setLevel(RiskLevel level) { this.level = level; }

    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}