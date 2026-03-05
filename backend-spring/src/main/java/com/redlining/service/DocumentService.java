package com.redlining.service;

import com.redlining.config.AppProperties;
import com.redlining.entity.Document;
import com.redlining.repository.DocumentRepository;
import com.redlining.security.JwtUtil;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class DocumentService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".docx", ".xlsx", ".pptx", ".doc", ".xls", ".ppt", ".odt", ".ods", ".odp");

    private final DocumentRepository documentRepository;
    private final MinioService minioService;
    private final AppProperties appProperties;
    private final JwtUtil jwtUtil;
    private final WebClient webClient;

    public DocumentService(DocumentRepository documentRepository,
                           MinioService minioService,
                           AppProperties appProperties,
                           JwtUtil jwtUtil,
                           WebClient.Builder webClientBuilder) {
        this.documentRepository = documentRepository;
        this.minioService = minioService;
        this.appProperties = appProperties;
        this.jwtUtil = jwtUtil;
        this.webClient = webClientBuilder.build();
    }

    public Document createDocument(Long userId, MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("File name is required");
        }
        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.')).toLowerCase() : "";
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("Unsupported document type. Allowed: " + ALLOWED_EXTENSIONS);
        }
        String bucket = appProperties.getMinio().getDocumentsBucket();
        String minioKey = "documents/" + userId + "/" + UUID.randomUUID() + "/" + name;
        try {
            minioService.putObjectInBucket(bucket, minioKey, file.getInputStream(), file.getSize(), file.getContentType());
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload document", e);
        }
        Document doc = new Document();
        doc.setUserId(userId);
        doc.setName(name);
        doc.setMinioKey(minioKey);
        return documentRepository.save(doc);
    }

    public Optional<Document> findByIdAndUserId(String id, Long userId) {
        return documentRepository.findByIdAndUserId(id, userId);
    }

    public java.util.List<Document> listByUserId(Long userId) {
        return documentRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /** Build OnlyOffice editor config (document.url = backend file URL with token; callbackUrl for save).
     * @param viewOnly when true, use OnlyOffice plugin in view/preview mode (no edit, no callback). */
    public Map<String, Object> getEditorConfig(String documentId, Long userId, boolean viewOnly) {
        Document doc = documentRepository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found"));
        String base = appProperties.getOnlyOffice().getApiBaseUrl().replaceAll("/$", "");
        String fileToken = jwtUtil.createDocumentFileToken(documentId);
        String documentUrl = base + "/api/documents/file?token=" + fileToken + "&documentId=" + documentId;

        String name = doc.getName();
        String fileType = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1).toLowerCase() : "";
        String documentType = documentTypeFromExtension(fileType);

        Map<String, Object> document = new HashMap<>();
        document.put("url", documentUrl);
        document.put("key", documentId);
        document.put("fileType", fileType);
        document.put("title", name);

        Map<String, Object> editorConfig = new HashMap<>();
        editorConfig.put("mode", viewOnly ? "view" : "edit");
        editorConfig.put("lang", "zh-CN");
        if (!viewOnly) {
            editorConfig.put("callbackUrl", base + "/api/documents/callback");
        }

        String documentServerUrl = appProperties.getOnlyOffice().getDocumentServerUrl().replaceAll("/$", "");
        Map<String, Object> config = new HashMap<>();
        config.put("document", document);
        config.put("documentType", documentType);
        config.put("editorConfig", editorConfig);
        config.put("documentServerUrl", documentServerUrl);
        return config;
    }

    private static String documentTypeFromExtension(String ext) {
        return switch (ext) {
            case "doc", "docx", "odt" -> "word";
            case "xls", "xlsx", "ods" -> "cell";
            case "ppt", "pptx", "odp" -> "slide";
            default -> "word";
        };
    }

    /** OnlyOffice callback: status 6 = saved, 7 = closed with changes. Download from url and save to MinIO. */
    public Map<String, Integer> handleCallback(Map<String, Object> body) {
        String key = (String) body.get("key");
        Object statusObj = body.get("status");
        int status = statusObj instanceof Number n ? n.intValue() : -1;
        String url = (String) body.get("url");

        if (key == null || key.isBlank()) {
            return Map.of("error", 1);
        }
        if (status != 6 && status != 7) {
            return Map.of("error", 0);
        }
        if (url == null || url.isBlank()) {
            return Map.of("error", 0);
        }

        Document doc = documentRepository.findById(key).orElse(null);
        if (doc == null) {
            return Map.of("error", 1);
        }

        byte[] bytes = webClient.get()
                .uri(URI.create(url))
                .retrieve()
                .bodyToMono(byte[].class)
                .block();
        if (bytes == null || bytes.length == 0) {
            return Map.of("error", 1);
        }

        String bucket = appProperties.getMinio().getDocumentsBucket();
        String contentType = "application/octet-stream";
        minioService.putObjectInBucket(bucket, doc.getMinioKey(), new java.io.ByteArrayInputStream(bytes), bytes.length, contentType);
        return Map.of("error", 0);
    }

    /** Stream document file after validating token (used by OnlyOffice to load document). Returns resource and doc for filename/contentType. */
    public FileStreamResult getFileByToken(String token, String documentId) {
        String parsedId = jwtUtil.parseDocumentIdFromFileToken(token);
        if (parsedId == null || !parsedId.equals(documentId)) {
            throw new IllegalArgumentException("Invalid or expired token");
        }
        Document doc = documentRepository.findById(documentId).orElseThrow(() -> new IllegalArgumentException("Document not found"));
        String bucket = appProperties.getMinio().getDocumentsBucket();
        InputStream stream = minioService.getObject(bucket, doc.getMinioKey());
        String contentType = getContentType(doc.getName());
        return new FileStreamResult(new InputStreamResource(stream), doc.getName(), contentType);
    }

    public record FileStreamResult(Resource resource, String filename, String contentType) {}

    public String getContentType(String filename) {
        if (filename == null) return "application/octet-stream";
        String ext = filename.contains(".") ? filename.substring(filename.lastIndexOf('.')).toLowerCase() : "";
        return switch (ext) {
            case ".docx", ".doc" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case ".xlsx", ".xls" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case ".pptx", ".ppt" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            default -> "application/octet-stream";
        };
    }
}
