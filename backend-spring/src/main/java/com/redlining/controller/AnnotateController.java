package com.redlining.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redlining.config.AppProperties;
import com.redlining.dto.AnalysisResultDto;
import com.redlining.entity.User;
import com.redlining.service.DocxAnnotationService;
import com.redlining.service.MinioService;
import com.redlining.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/api")
public class AnnotateController {

    private static final Logger log = LoggerFactory.getLogger(AnnotateController.class);
    private static final Set<String> DOCX_EXTENSIONS = Set.of(".docx", ".doc");
    private static final long MAX_SIZE = 50 * 1024 * 1024L; // 50MB

    private final DocxAnnotationService docxAnnotationService;
    private final MinioService minioService;
    private final JwtUtil jwtUtil;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    public AnnotateController(DocxAnnotationService docxAnnotationService,
                             MinioService minioService,
                             JwtUtil jwtUtil,
                             AppProperties appProperties,
                             ObjectMapper objectMapper) {
        this.docxAnnotationService = docxAnnotationService;
        this.minioService = minioService;
        this.jwtUtil = jwtUtil;
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
    }

    @PostMapping(value = "/annotate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> annotate(
            @AuthenticationPrincipal User user,
            @RequestParam("file") MultipartFile file,
            @RequestParam("analysisResult") String analysisResultJson
    ) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        String name = file.getOriginalFilename();
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "File name is required");
        }
        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.')).toLowerCase() : "";
        if (!DOCX_EXTENSIONS.contains(ext)) {
            throw new ResponseStatusException(BAD_REQUEST, "Only DOCX/DOC files support auto-annotation");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new ResponseStatusException(BAD_REQUEST, "File too large");
        }
        AnalysisResultDto result;
        try {
            result = objectMapper.readValue(analysisResultJson, AnalysisResultDto.class);
        } catch (Exception e) {
            log.warn("Failed to parse analysisResult: {}", e.getMessage());
            throw new ResponseStatusException(BAD_REQUEST, "Invalid analysisResult JSON");
        }
        if (result.getRisks() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "analysisResult.risks is required");
        }
        int riskCount = result.getRisks().size();
        log.info("Annotate: request for file={}, risks={}", name, riskCount);
        byte[] annotatedBytes;
        try {
            annotatedBytes = docxAnnotationService.annotate(file.getInputStream(), result);
        } catch (Exception e) {
            log.error("Annotation failed", e);
            throw new ResponseStatusException(BAD_REQUEST, "Failed to add comments to document: " + e.getMessage());
        }
        String bucket = appProperties.getMinio().getDocumentsBucket();
        String minioKey = "annotate/" + UUID.randomUUID() + "/" + name;
        try {
            minioService.putObjectInBucket(bucket, minioKey,
                    new ByteArrayInputStream(annotatedBytes), annotatedBytes.length,
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        } catch (Exception e) {
            log.warn("Annotated file storage failed: {}", e.getMessage());
            String cause = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            throw new ResponseStatusException(BAD_REQUEST,
                    "Failed to store annotated file. Is MinIO running? Cause: " + (cause != null ? cause : "unknown"));
        }
        String token = jwtUtil.createPreviewFileToken(minioKey);
        String base = appProperties.getOnlyOffice().getApiBaseUrl().replaceAll("/$", "");
        String documentUrl = base + "/api/preview/file?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
        String documentKey = UUID.randomUUID().toString().replace("-", "");

        Map<String, Object> document = new HashMap<>();
        document.put("url", documentUrl);
        document.put("key", documentKey);
        document.put("fileType", "docx");
        document.put("title", name);
        Map<String, Object> permissions = new HashMap<>();
        permissions.put("edit", true);
        permissions.put("comment", true);
        document.put("permissions", permissions);

        Map<String, Object> editorConfig = new HashMap<>();
        editorConfig.put("mode", "edit");
        editorConfig.put("lang", "zh-CN");
        Map<String, Object> editorUser = new HashMap<>();
        editorUser.put("id", "redlining-viewer");
        editorUser.put("name", "合同审查");
        editorConfig.put("user", editorUser);
        Map<String, Object> customization = new HashMap<>();
        customization.put("comments", true);
        customization.put("reviewDisplay", "markup");
        editorConfig.put("customization", customization);

        String documentServerUrl = appProperties.getOnlyOffice().getDocumentServerUrl().replaceAll("/$", "");
        Map<String, Object> config = new HashMap<>();
        config.put("document", document);
        config.put("documentType", "word");
        config.put("editorConfig", editorConfig);
        config.put("documentServerUrl", documentServerUrl);
        config.put("minioKey", minioKey);
        log.info("Annotate: stored key={} for user", minioKey);
        return ResponseEntity.ok(config);
    }
}
