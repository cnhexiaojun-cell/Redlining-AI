package com.redlining.controller;

import com.redlining.dto.AnalysisResultDto;
import com.redlining.entity.User;
import com.redlining.service.ContractAnalyzerService;
import com.redlining.service.FileExtractService;
import com.redlining.service.ReportService;
import com.redlining.service.UsageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@RestController
@RequestMapping("/api")
public class AnalyzeController {

    private static final Logger log = LoggerFactory.getLogger(AnalyzeController.class);

    private final FileExtractService fileExtractService;
    private final ContractAnalyzerService contractAnalyzerService;
    private final UsageService usageService;
    private final ReportService reportService;

    public AnalyzeController(FileExtractService fileExtractService,
                             ContractAnalyzerService contractAnalyzerService,
                             UsageService usageService,
                             ReportService reportService) {
        this.fileExtractService = fileExtractService;
        this.contractAnalyzerService = contractAnalyzerService;
        this.usageService = usageService;
        this.reportService = reportService;
    }

    @PostMapping("/analyze")
    public ResponseEntity<AnalysisResultDto> analyze(
            @AuthenticationPrincipal User user,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "stance", required = false) String stance,
            @RequestParam(value = "advanced_rules", required = false) String advancedRules
    ) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        usageService.consumeOneAnalysis(user);
        String contractText = fileExtractService.extractText(file);
        if (contractText == null || contractText.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No text could be extracted from the file");
        }
        String contentFingerprint = contentFingerprint(contractText);
        log.info("analyze: file={} size={} extractedLength={} fingerprint={}",
                file.getOriginalFilename(), file.getSize(), contractText.length(), contentFingerprint);
        AnalysisResultDto result = contractAnalyzerService.analyze(contractText, stance, advancedRules);
        String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "contract";
        Long reportId = reportService.saveReport(user.getId(), fileName, result);
        result.setReportId(reportId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/extract")
    public ResponseEntity<Map<String, String>> extract(
            @AuthenticationPrincipal User user,
            @RequestParam("file") MultipartFile file
    ) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        String contractText = fileExtractService.extractText(file);
        if (contractText == null || contractText.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No text could be extracted from the file");
        }
        return ResponseEntity.ok(Map.of("text", contractText));
    }

    private static String contentFingerprint(String text) {
        if (text == null || text.isEmpty()) return "empty";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            return "len:" + text.length();
        }
    }
}
