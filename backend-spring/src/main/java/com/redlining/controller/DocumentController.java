package com.redlining.controller;

import com.redlining.entity.Document;
import com.redlining.entity.User;
import com.redlining.service.DocumentService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Document> create(
            @AuthenticationPrincipal User user,
            @RequestParam("file") MultipartFile file
    ) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        try {
            Document doc = documentService.createDocument(user.getId(), file);
            return ResponseEntity.ok(doc);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<Document>> list(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(documentService.listByUserId(user.getId()));
    }

    @GetMapping("/{id}/editor-config")
    public ResponseEntity<Map<String, Object>> getEditorConfig(
            @AuthenticationPrincipal User user,
            @PathVariable String id,
            @RequestParam(name = "mode", defaultValue = "edit") String mode
    ) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        boolean viewOnly = "view".equalsIgnoreCase(mode);
        try {
            return ResponseEntity.ok(documentService.getEditorConfig(id, user.getId(), viewOnly));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(NOT_FOUND, e.getMessage());
        }
    }

    @PostMapping(value = "/callback", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Integer>> callback(@RequestBody Map<String, Object> body) {
        Map<String, Integer> result = documentService.handleCallback(body);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/file")
    public ResponseEntity<Resource> getFile(
            @RequestParam String token,
            @RequestParam String documentId
    ) {
        try {
            DocumentService.FileStreamResult result = documentService.getFileByToken(token, documentId);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(result.contentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.filename() + "\"")
                    .body(result.resource());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(BAD_REQUEST, e.getMessage());
        }
    }
}
