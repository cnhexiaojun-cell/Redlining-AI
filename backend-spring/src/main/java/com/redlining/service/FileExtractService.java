package com.redlining.service;

import com.redlining.config.AppProperties;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;

@Service
public class FileExtractService {

    private final AppProperties appProperties;

    public FileExtractService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public String extractText(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing filename");
        }
        String suffix = filename.substring(filename.lastIndexOf('.')).toLowerCase(Locale.ROOT);
        String[] allowed = appProperties.getFile().getAllowedExtensionsArray();
        if (allowed.length > 0 && !Arrays.asList(allowed).contains(suffix)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unsupported file type. Allowed: " + String.join(", ", allowed));
        }
        if (file.getSize() > appProperties.getFile().getMaxSize()) {
            long maxMb = appProperties.getFile().getMaxSize() / (1024 * 1024);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "File too large. Max size: " + maxMb + "MB");
        }
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        }

        return switch (suffix) {
            case ".pdf" -> readPdf(file);
            case ".docx" -> readDocx(file);
            case ".txt" -> readTxt(file);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported extension");
        };
    }

    private String readPdf(MultipartFile file) {
        try {
            try (InputStream is = file.getInputStream(); PDDocument doc = Loader.loadPDF(new RandomAccessReadBuffer(is))) {
                PDFTextStripper stripper = new PDFTextStripper();
                return stripper.getText(doc).strip();
            }
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to read PDF: " + e.getMessage());
        }
    }

    private String readDocx(MultipartFile file) {
        try {
            try (InputStream is = file.getInputStream(); XWPFDocument doc = new XWPFDocument(is)) {
                StringBuilder sb = new StringBuilder();
                for (XWPFParagraph p : doc.getParagraphs()) {
                    if (p.getText() != null) {
                        sb.append(p.getText()).append("\n");
                    }
                }
                return sb.toString().strip();
            }
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to read Word document: " + e.getMessage());
        }
    }

    private String readTxt(MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            try {
                return new String(bytes, StandardCharsets.UTF_8).strip();
            } catch (Exception ignored) {
                return new String(bytes, Charset.forName("GB2312")).replace("\uFFFD", "").strip();
            }
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to read text file: " + e.getMessage());
        }
    }
}
