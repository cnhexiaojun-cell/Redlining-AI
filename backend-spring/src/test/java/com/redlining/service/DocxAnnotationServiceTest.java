package com.redlining.service;

import com.redlining.dto.AnalysisResultDto;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for DOCX comment (批注) insertion. Verifies that annotate() completes without
 * exception, produces valid DOCX, and that comment ranges are present so Word/OnlyOffice can display them.
 */
class DocxAnnotationServiceTest {

    private DocxAnnotationService service;

    @BeforeEach
    void setUp() {
        service = new DocxAnnotationService();
    }

    @Test
    void annotate_emptyRisks_returnsUnchangedDocx() throws Exception {
        byte[] docxBytes = minimalDocx("Hello");
        AnalysisResultDto result = new AnalysisResultDto();
        result.setRisks(List.of());

        byte[] out = service.annotate(new ByteArrayInputStream(docxBytes), result);

        assertThat(out).isNotEmpty();
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(out))) {
            assertThat(doc.getParagraphs()).hasSize(1);
            assertThat(doc.getParagraphs().get(0).getText()).isEqualTo("Hello");
            assertThat(doc.getDocComments()).isNull();
        }
    }

    @Test
    void annotate_withMatchingRisk_insertsCommentAndCommentRange() throws Exception {
        String paragraphText = "2.1 甲方责任";
        byte[] docxBytes = minimalDocx(paragraphText);

        AnalysisResultDto result = new AnalysisResultDto();
        AnalysisResultDto.RiskItem risk = new AnalysisResultDto.RiskItem();
        risk.setClause("2.1");
        risk.setSeverity("高");
        risk.setDescription("风险描述");
        risk.setSuggestion("建议");
        result.setRisks(List.of(risk));

        byte[] out = service.annotate(new ByteArrayInputStream(docxBytes), result);

        assertThat(out).isNotEmpty();
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(out))) {
            assertThat(doc.getDocComments()).isNotNull();
            assertThat(doc.getDocComments().getComments()).hasSize(1);
            assertThat(doc.getDocComments().getComments().get(0).getText()).contains("风险描述");

            // Paragraph in body must have comment range so OnlyOffice/Word shows the comment.
            boolean foundCommentRange = false;
            for (XWPFParagraph p : doc.getParagraphs()) {
                String ctpXml = p.getCTP().xmlText();
                if (ctpXml.contains("commentRangeStart") && ctpXml.contains("commentRangeEnd")) {
                    // commentRangeStart should appear before the main text content (w:t) so the range wraps the paragraph.
                    int startIdx = ctpXml.indexOf("commentRangeStart");
                    int endIdx = ctpXml.indexOf("commentRangeEnd");
                    int textIdx = ctpXml.indexOf("w:t");
                    assertThat(startIdx).isLessThan(endIdx);
                    if (textIdx > 0) {
                        assertThat(startIdx).isLessThan(textIdx);
                    }
                    foundCommentRange = true;
                    break;
                }
            }
            assertThat(foundCommentRange).as("At least one paragraph must have comment range (commentRangeStart before content)").isTrue();
        }
    }

    @Test
    void annotate_multipleRisks_insertsAllMatchingComments() throws Exception {
        byte[] docxBytes = minimalDocxWithTwoParagraphs("2.1 第一条", "4.2 第二条");

        AnalysisResultDto result = new AnalysisResultDto();
        AnalysisResultDto.RiskItem r1 = new AnalysisResultDto.RiskItem();
        r1.setClause("2.1");
        r1.setSeverity("中");
        r1.setDescription("desc1");
        result.setRisks(List.of(r1, riskItem("4.2", "desc2")));

        byte[] out = service.annotate(new ByteArrayInputStream(docxBytes), result);

        assertThat(out).isNotEmpty();
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(out))) {
            assertThat(doc.getDocComments()).isNotNull();
            assertThat(doc.getDocComments().getComments()).hasSize(2);
            int withRanges = 0;
            for (XWPFParagraph p : doc.getParagraphs()) {
                if (p.getCTP().xmlText().contains("commentRangeStart")) withRanges++;
            }
            assertThat(withRanges).isEqualTo(2);
        }
    }

    @Test
    void annotate_noMatchingParagraph_completesWithoutException() throws Exception {
        byte[] docxBytes = minimalDocx("Unrelated text");

        AnalysisResultDto result = new AnalysisResultDto();
        AnalysisResultDto.RiskItem risk = new AnalysisResultDto.RiskItem();
        risk.setClause("NonExistentClause123");
        result.setRisks(List.of(risk));

        byte[] out = service.annotate(new ByteArrayInputStream(docxBytes), result);

        assertThat(out).isNotEmpty();
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(out))) {
            assertThat(doc.getParagraphs().get(0).getText()).isEqualTo("Unrelated text");
            // When no paragraph matches, service may still create empty comments part
            if (doc.getDocComments() != null) {
                assertThat(doc.getDocComments().getComments()).isEmpty();
            }
        }
    }

    private static byte[] minimalDocx(String paragraphText) throws Exception {
        try (XWPFDocument doc = new XWPFDocument()) {
            XWPFParagraph p = doc.createParagraph();
            p.createRun().setText(paragraphText);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.write(out);
            return out.toByteArray();
        }
    }

    private static byte[] minimalDocxWithTwoParagraphs(String text1, String text2) throws Exception {
        try (XWPFDocument doc = new XWPFDocument()) {
            doc.createParagraph().createRun().setText(text1);
            doc.createParagraph().createRun().setText(text2);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.write(out);
            return out.toByteArray();
        }
    }

    private static AnalysisResultDto.RiskItem riskItem(String clause, String desc) {
        AnalysisResultDto.RiskItem r = new AnalysisResultDto.RiskItem();
        r.setClause(clause);
        r.setDescription(desc);
        return r;
    }
}
