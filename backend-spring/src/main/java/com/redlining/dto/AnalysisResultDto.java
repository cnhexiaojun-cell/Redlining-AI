package com.redlining.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnalysisResultDto {

    private String summary;
    private Integer compliance_score;
    private List<RiskItem> risks;
    private List<MissingClause> missing_clauses;
    private List<String> key_points;
    /** When set, frontend can show a notice e.g. "未配置 AI，此为基于合同内容的示例结果" */
    private String notice;

    /** Id of the saved report record; set by backend after persisting. */
    private Long reportId;

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Integer getCompliance_score() {
        return compliance_score;
    }

    public void setCompliance_score(Integer compliance_score) {
        this.compliance_score = compliance_score;
    }

    public List<RiskItem> getRisks() {
        return risks;
    }

    public void setRisks(List<RiskItem> risks) {
        this.risks = risks;
    }

    public List<MissingClause> getMissing_clauses() {
        return missing_clauses;
    }

    public void setMissing_clauses(List<MissingClause> missing_clauses) {
        this.missing_clauses = missing_clauses;
    }

    public List<String> getKey_points() {
        return key_points;
    }

    public void setKey_points(List<String> key_points) {
        this.key_points = key_points;
    }

    public String getNotice() {
        return notice;
    }

    public void setNotice(String notice) {
        this.notice = notice;
    }

    public Long getReportId() {
        return reportId;
    }

    public void setReportId(Long reportId) {
        this.reportId = reportId;
    }

    public static class RiskItem {
        private String type;
        private String description;
        private String severity;
        private String clause;
        private String suggestion;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getSeverity() {
            return severity;
        }

        public void setSeverity(String severity) {
            this.severity = severity;
        }

        public String getClause() {
            return clause;
        }

        public void setClause(String clause) {
            this.clause = clause;
        }

        public String getSuggestion() {
            return suggestion;
        }

        public void setSuggestion(String suggestion) {
            this.suggestion = suggestion;
        }
    }

    public static class MissingClause {
        private String clause;
        private String importance;
        private String recommendation;

        public String getClause() {
            return clause;
        }

        public void setClause(String clause) {
            this.clause = clause;
        }

        public String getImportance() {
            return importance;
        }

        public void setImportance(String importance) {
            this.importance = importance;
        }

        public String getRecommendation() {
            return recommendation;
        }

        public void setRecommendation(String recommendation) {
            this.recommendation = recommendation;
        }
    }
}
