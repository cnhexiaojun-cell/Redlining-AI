package com.redlining.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "reports", indexes = {
    @Index(columnList = "user_id"),
    @Index(columnList = "created_at")
})
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "compliance_score", nullable = true)
    private Integer complianceScore;

    @Column(name = "risk_count_high", nullable = false)
    private int riskCountHigh = 0;

    @Column(name = "risk_count_medium_high", nullable = false)
    private int riskCountMediumHigh = 0;

    @Column(name = "risk_count_medium", nullable = false)
    private int riskCountMedium = 0;

    @Column(name = "risk_count_low", nullable = false)
    private int riskCountLow = 0;

    @Column(name = "result_json", nullable = false, columnDefinition = "TEXT")
    private String resultJson;

    /** MinIO key of preview/annotated document for OnlyOffice (e.g. preview/uuid/name.pdf or annotate/uuid/name.docx). */
    @Column(name = "document_key", length = 512)
    private String documentKey;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
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
    public String getResultJson() { return resultJson; }
    public void setResultJson(String resultJson) { this.resultJson = resultJson; }
    public String getDocumentKey() { return documentKey; }
    public void setDocumentKey(String documentKey) { this.documentKey = documentKey; }
}
