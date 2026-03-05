package com.redlining.dto;

import java.time.Instant;

public class ReportDto {
    private Long id;
    private String fileName;
    private Instant createdAt;
    private Integer complianceScore;
    private int riskCountHigh;
    private int riskCountMediumHigh;
    private int riskCountMedium;
    private int riskCountLow;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Integer getComplianceScore() { return complianceScore; }
    public void setComplianceScore(Integer complianceScore) { this.complianceScore = complianceScore; }
    public int getRiskCountHigh() { return riskCountHigh; }
    public void setRiskCountHigh(int riskCountHigh) { this.riskCountHigh = riskCountHigh; }
    public int getRiskCountMediumHigh() { return riskCountMediumHigh; }
    public void setRiskCountMediumHigh(int riskCountMediumHigh) { this.riskCountMediumHigh = riskCountMediumHigh; }
    public int getRiskCountMedium() { return riskCountMedium; }
    public void setRiskCountMedium(int riskCountMedium) { this.riskCountMedium = riskCountMedium; }
    public int getRiskCountLow() { return riskCountLow; }
    public void setRiskCountLow(int riskCountLow) { this.riskCountLow = riskCountLow; }
}
