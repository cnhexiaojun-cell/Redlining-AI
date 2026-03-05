package com.redlining.dto;

import java.util.Map;

/** Report with full analysis result for detail view. */
public class ReportDetailDto extends ReportDto {

    private AnalysisResultDto result;
    /** OnlyOffice editor config when report has linked document (documentKey). */
    private Map<String, Object> onlyOfficeConfig;

    public ReportDetailDto() {
    }

    public AnalysisResultDto getResult() {
        return result;
    }

    public void setResult(AnalysisResultDto result) {
        this.result = result;
    }

    public Map<String, Object> getOnlyOfficeConfig() {
        return onlyOfficeConfig;
    }

    public void setOnlyOfficeConfig(Map<String, Object> onlyOfficeConfig) {
        this.onlyOfficeConfig = onlyOfficeConfig;
    }
}
