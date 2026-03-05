package com.redlining.controller;

import com.redlining.config.AppProperties;
import com.redlining.entity.User;
import com.redlining.service.MinioService;
import com.redlining.security.JwtUtil;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/api/preview")
public class PreviewController {

    private static final Logger log = LoggerFactory.getLogger(PreviewController.class);
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".pdf", ".docx", ".doc");
    private static final long MAX_SIZE = 50 * 1024 * 1024L; // 50MB

    private final MinioService minioService;
    private final JwtUtil jwtUtil;
    private final AppProperties appProperties;

    public PreviewController(MinioService minioService, JwtUtil jwtUtil, AppProperties appProperties) {
        this.minioService = minioService;
        this.jwtUtil = jwtUtil;
        this.appProperties = appProperties;
    }

    /**
     * Upload a contract file for OnlyOffice preview (view-only) on the upload page.
     * Returns OnlyOffice editor config so the frontend can render the document with OnlyOffice plugin.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadForPreview(
            @AuthenticationPrincipal User user,
            @RequestParam("file") MultipartFile file
    ) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        String name = file.getOriginalFilename();
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "File name is required");
        }
        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.')).toLowerCase() : "";
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new ResponseStatusException(BAD_REQUEST, "Preview supports PDF, DOCX, DOC only");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new ResponseStatusException(BAD_REQUEST, "File too large");
        }
        String bucket = appProperties.getMinio().getDocumentsBucket();
        String minioKey = "preview/" + UUID.randomUUID() + "/" + name;
        try {
            minioService.putObjectInBucket(bucket, minioKey, file.getInputStream(), file.getSize(), file.getContentType());
        } catch (Exception e) {
            log.warn("Preview storage failed: {}", e.getMessage());
            String cause = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            throw new ResponseStatusException(BAD_REQUEST,
                    "Preview storage failed. Is MinIO running? (e.g. docker run -p 9000:9000 minio/minio server /data). Cause: " + (cause != null ? cause : "unknown"));
        }
        String token = jwtUtil.createPreviewFileToken(minioKey);
        String base = appProperties.getOnlyOffice().getApiBaseUrl().replaceAll("/$", "");
        String documentUrl = base + "/api/preview/file?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
        String fileType = ext.substring(1);
        String documentType = "pdf".equalsIgnoreCase(fileType) ? "pdf" : "word";
        // OnlyOffice document.key: only 0-9, a-z, A-Z, -._=, max 128 chars (no slashes)
        String documentKey = UUID.randomUUID().toString().replace("-", "");

        Map<String, Object> document = new HashMap<>();
        document.put("url", documentUrl);
        document.put("key", documentKey);
        document.put("fileType", fileType);
        document.put("title", name);
        if ("pdf".equals(documentType)) {
            document.put("isForm", false);
        }

        Map<String, Object> editorConfig = new HashMap<>();
        editorConfig.put("mode", "view");
        editorConfig.put("lang", "zh-CN");

        String documentServerUrl = appProperties.getOnlyOffice().getDocumentServerUrl().replaceAll("/$", "");
        Map<String, Object> config = new HashMap<>();
        config.put("document", document);
        config.put("documentType", documentType);
        config.put("editorConfig", editorConfig);
        config.put("documentServerUrl", documentServerUrl);
        config.put("minioKey", minioKey);
        return ResponseEntity.ok(config);
    }

    /**
     * Stream the preview file (OnlyOffice Document Server fetches from this URL).
     */
    @GetMapping("/file")
    public ResponseEntity<Resource> getFile(@RequestParam(required = false) String token) {
        if (token == null || token.isBlank()) {
            log.warn("Preview file: missing token");
            throw new ResponseStatusException(BAD_REQUEST, "Missing token");
        }
        log.info("Preview file requested (token length={})", token.length());
        // Some HTTP clients decode query string and turn + into space; JWT uses + so restore it
        String tokenToUse = token.contains(" ") ? token.replace(" ", "+") : token;
        String minioKey = jwtUtil.parsePreviewKeyFromToken(tokenToUse);
        if (minioKey == null || minioKey.isBlank()) {
            log.warn("Preview file: invalid or expired token");
            throw new ResponseStatusException(BAD_REQUEST, "Invalid or expired token");
        }
        String bucket = appProperties.getMinio().getDocumentsBucket();
        try {
            Resource resource = new InputStreamResource(minioService.getObject(bucket, minioKey));
            String filename = minioKey.contains("/") ? minioKey.substring(minioKey.lastIndexOf('/') + 1) : "document";
            String contentType = contentTypeFromFilename(filename);
            log.info("Preview file: serving key={}", minioKey);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(resource);
        } catch (Exception e) {
            log.warn("Preview file: not found key={}", minioKey, e);
            throw new ResponseStatusException(BAD_REQUEST, "File not found");
        }
    }

    private static String contentTypeFromFilename(String filename) {
        if (filename == null) return "application/octet-stream";
        String ext = filename.contains(".") ? filename.substring(filename.lastIndexOf('.')).toLowerCase() : "";
        return switch (ext) {
            case ".pdf" -> "application/pdf";
            case ".docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case ".doc" -> "application/msword";
            default -> "application/octet-stream";
        };
    }
}
