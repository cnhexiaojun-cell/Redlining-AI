package com.redlining.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redlining.config.AppProperties;
import com.redlining.dto.AnalysisResultDto;
import com.redlining.dto.ReportDetailDto;
import com.redlining.dto.ReportDto;
import com.redlining.entity.Report;
import com.redlining.repository.ReportRepository;
import com.redlining.security.JwtUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {

    private final ReportRepository reportRepository;
    private final ObjectMapper objectMapper;
    private final JwtUtil jwtUtil;
    private final AppProperties appProperties;

    public ReportService(ReportRepository reportRepository, ObjectMapper objectMapper,
                         JwtUtil jwtUtil, AppProperties appProperties) {
        this.reportRepository = reportRepository;
        this.objectMapper = objectMapper;
        this.jwtUtil = jwtUtil;
        this.appProperties = appProperties;
    }

    /**
     * Save analysis result as a report and return its id.
     */
    public Long saveReport(Long userId, String fileName, AnalysisResultDto dto) {
        int high = 0, mediumHigh = 0, medium = 0, low = 0;
        List<AnalysisResultDto.RiskItem> risks = dto.getRisks();
        if (risks != null) {
            for (AnalysisResultDto.RiskItem r : risks) {
                String sev = r.getSeverity();
                if (sev == null) {
                    low++;
                } else if ("高".equals(sev)) {
                    high++;
                } else if ("中高".equals(sev)) {
                    mediumHigh++;
                } else if ("中".equals(sev)) {
                    medium++;
                } else {
                    low++;
                }
            }
        }
        String resultJson;
        try {
            resultJson = objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize analysis result", e);
        }
        Report report = new Report();
        report.setUserId(userId);
        report.setFileName(fileName != null ? fileName : "contract");
        report.setComplianceScore(dto.getCompliance_score());
        report.setRiskCountHigh(high);
        report.setRiskCountMediumHigh(mediumHigh);
        report.setRiskCountMedium(medium);
        report.setRiskCountLow(low);
        report.setResultJson(resultJson);
        report = reportRepository.save(report);
        return report.getId();
    }

    public Page<ReportDto> list(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(50, Math.max(1, size)));
        return reportRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toDto);
    }

    public void updateDocumentKey(Long id, Long userId, String documentKey) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));
        if (!report.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Report does not belong to user");
        }
        if (documentKey == null || documentKey.isBlank()
                || (!documentKey.startsWith("preview/") && !documentKey.startsWith("annotate/"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid documentKey");
        }
        report.setDocumentKey(documentKey);
        reportRepository.save(report);
    }

    public ReportDetailDto get(Long id, Long userId) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));
        if (!report.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Report does not belong to user");
        }
        ReportDetailDto dto = new ReportDetailDto();
        toDto(report, dto);
        try {
            AnalysisResultDto result = objectMapper.readValue(report.getResultJson(), AnalysisResultDto.class);
            dto.setResult(result);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize report result", e);
        }
        String docKey = report.getDocumentKey();
        if (docKey != null && !docKey.isBlank()) {
            Map<String, Object> config = buildOnlyOfficeConfig(report.getId(), docKey, report.getFileName());
            if (config != null) {
                dto.setOnlyOfficeConfig(config);
            }
        }
        return dto;
    }

    private Map<String, Object> buildOnlyOfficeConfig(Long reportId, String minioKey, String fileName) {
        try {
            String base = appProperties.getOnlyOffice().getApiBaseUrl();
            if (base == null || base.isBlank()) {
                return null;
            }
            base = base.replaceAll("/$", "");
            String token = jwtUtil.createPreviewFileToken(minioKey);
            String documentUrl = base + "/api/preview/file?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
            String ext = minioKey.contains(".") ? minioKey.substring(minioKey.lastIndexOf('.')).toLowerCase() : "";
            String fileType = ext.isEmpty() ? "pdf" : ext.substring(1);
            String documentType = "pdf".equalsIgnoreCase(fileType) ? "pdf" : "word";
            String onlyOfficeDocKey = "report-" + reportId;

            Map<String, Object> document = new HashMap<>();
            document.put("url", documentUrl);
            document.put("key", onlyOfficeDocKey);
            document.put("fileType", fileType);
            document.put("title", fileName != null ? fileName : "document");
            if ("pdf".equals(documentType)) {
                document.put("isForm", false);
            }
            boolean isAnnotate = minioKey.startsWith("annotate/");
            if (isAnnotate) {
                Map<String, Object> permissions = new HashMap<>();
                permissions.put("edit", true);
                permissions.put("comment", true);
                document.put("permissions", permissions);
            }

            Map<String, Object> editorConfig = new HashMap<>();
            editorConfig.put("mode", isAnnotate ? "edit" : "view");
            editorConfig.put("lang", "zh-CN");
            if (isAnnotate) {
                Map<String, Object> user = new HashMap<>();
                user.put("id", "redlining-viewer");
                user.put("name", "合同审查");
                editorConfig.put("user", user);
                Map<String, Object> customization = new HashMap<>();
                customization.put("comments", true);
                customization.put("reviewDisplay", "markup");
                editorConfig.put("customization", customization);
            }

            String documentServerUrl = appProperties.getOnlyOffice().getDocumentServerUrl();
            if (documentServerUrl == null || documentServerUrl.isBlank()) {
                return null;
            }
            documentServerUrl = documentServerUrl.replaceAll("/$", "");
            Map<String, Object> config = new HashMap<>();
            config.put("document", document);
            config.put("documentType", documentType);
            config.put("editorConfig", editorConfig);
            config.put("documentServerUrl", documentServerUrl);
            return config;
        } catch (Exception e) {
            return null;
        }
    }

    private ReportDto toDto(Report r) {
        ReportDto dto = new ReportDto();
        toDto(r, dto);
        return dto;
    }

    private void toDto(Report r, ReportDto dto) {
        dto.setId(r.getId());
        dto.setFileName(r.getFileName());
        dto.setCreatedAt(r.getCreatedAt());
        dto.setComplianceScore(r.getComplianceScore());
        dto.setRiskCountHigh(r.getRiskCountHigh());
        dto.setRiskCountMediumHigh(r.getRiskCountMediumHigh());
        dto.setRiskCountMedium(r.getRiskCountMedium());
        dto.setRiskCountLow(r.getRiskCountLow());
    }
}
