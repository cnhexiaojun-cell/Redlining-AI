package com.redlining.controller;

import com.redlining.dto.ReportDetailDto;
import com.redlining.dto.ReportDto;
import com.redlining.entity.User;
import com.redlining.service.ReportService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PutMapping("/{id}/document")
    public ResponseEntity<Void> linkDocument(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String documentKey = body != null ? body.get("documentKey") : null;
        reportService.updateDocumentKey(id, user.getId(), documentKey);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<Page<ReportDto>> list(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(reportService.list(user.getId(), page, size));
    }

    @GetMapping("/{id}")
    public ReportDetailDto get(@AuthenticationPrincipal User user, @PathVariable Long id) {
        if (user == null) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return reportService.get(id, user.getId());
    }
}
