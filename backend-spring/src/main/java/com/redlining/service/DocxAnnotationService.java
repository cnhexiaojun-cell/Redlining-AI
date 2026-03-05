package com.redlining.service;

import com.redlining.dto.AnalysisResultDto;
import org.apache.poi.xwpf.usermodel.XWPFComment;
import org.apache.poi.xwpf.usermodel.XWPFComments;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Pattern;
import javax.xml.namespace.QName;

/**
 * Inserts AI review comments (批注) into DOCX at clause positions.
 * CommentRangeStart is inserted at the beginning of the paragraph so the range
 * wraps the paragraph text and OnlyOffice displays the comment correctly.
 */
@Service
public class DocxAnnotationService {

    private static final Logger log = LoggerFactory.getLogger(DocxAnnotationService.class);
    private static final int MAX_COMMENT_CHARS = 500;
    private static final String TRUNCATE_SUFFIX = "（内容已截断）";

    private static final String AUTHOR_HIGH = "AI-高风险";
    private static final String AUTHOR_MEDIUM = "AI-中风险";
    private static final String AUTHOR_LOW = "AI-低风险";

    public byte[] annotate(InputStream docxInput, AnalysisResultDto result) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(docxInput)) {
            if (result.getRisks() == null || result.getRisks().isEmpty()) {
                log.info("Annotate: no risks, returning document unchanged");
                return toBytes(doc);
            }
            ensureCommentsPart(doc);
            List<XWPFParagraph> allParagraphs = collectAllParagraphsInOrder(doc);
            int inserted = 0;
            int noMatch = 0;
            for (AnalysisResultDto.RiskItem risk : result.getRisks()) {
                String clause = normalizeWhitespace(risk.getClause());
                if (clause == null || clause.isBlank()) continue;
                String commentText = buildCommentText(risk);
                if (commentText.length() > MAX_COMMENT_CHARS) {
                    commentText = commentText.substring(0, MAX_COMMENT_CHARS - TRUNCATE_SUFFIX.length()) + TRUNCATE_SUFFIX;
                }
                String author = authorBySeverity(risk.getSeverity());
                XWPFParagraph best = findBestMatchingParagraph(allParagraphs, clause);
                if (best != null) {
                    BigInteger commentId = addCommentToDocument(doc, commentText, author);
                    insertCommentRangeInParagraph(best, commentId, clause);
                    inserted++;
                    log.info("Annotate: inserted comment id={} for clause \"{}\"", commentId, clause.substring(0, Math.min(40, clause.length())) + (clause.length() > 40 ? "..." : ""));
                } else {
                    noMatch++;
                }
            }
            log.info("Annotate: done. inserted={}, risksWithoutMatch={}, totalParagraphs={}", inserted, noMatch, allParagraphs.size());
            return toBytes(doc);
        }
    }

    private static String normalizeWhitespace(String s) {
        if (s == null) return "";
        return Pattern.compile("\\s+").matcher(s.trim()).replaceAll(" ").trim();
    }

    private static boolean fuzzyContains(String paragraph, String clause) {
        if (clause.length() > paragraph.length()) return false;
        if (clause.length() < 10) return paragraph.contains(clause);
        String sub = clause.length() > 50 ? clause.substring(0, 50) : clause;
        return paragraph.contains(sub);
    }

    /** Collect body paragraphs and all paragraphs inside table cells, in document order, so clauses in tables get comments. */
    private static List<XWPFParagraph> collectAllParagraphsInOrder(XWPFDocument doc) {
        List<XWPFParagraph> out = new ArrayList<>();
        for (var element : doc.getBodyElements()) {
            if (element instanceof XWPFParagraph) {
                out.add((XWPFParagraph) element);
            } else if (element instanceof XWPFTable) {
                for (XWPFTableRow row : ((XWPFTable) element).getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        out.addAll(cell.getParagraphs());
                    }
                }
            }
        }
        return out;
    }

    /**
     * Find the paragraph that best corresponds to the clause so the comment anchor shows on the right text.
     * Prefer: 1) paragraph that starts with the clause, 2) paragraph containing full clause with longer overlap, 3) first match.
     */
    private static XWPFParagraph findBestMatchingParagraph(List<XWPFParagraph> paragraphs, String clause) {
        XWPFParagraph best = null;
        int bestScore = -1;
        for (XWPFParagraph p : paragraphs) {
            String pText = p.getText();
            if (pText == null) continue;
            String normalized = normalizeWhitespace(pText);
            if (!normalized.contains(clause) && !clause.contains(normalized) && !fuzzyContains(normalized, clause)) continue;
            int score = 0;
            if (normalized.startsWith(clause) || normalized.equals(clause)) score = 1000;
            else if (clause.length() >= 10 && normalized.contains(clause)) score = 500 + Math.min(clause.length(), 200);
            else if (normalized.contains(clause)) score = 300;
            else score = 100;
            if (score > bestScore) {
                bestScore = score;
                best = p;
            }
        }
        return best;
    }

    private static String buildCommentText(AnalysisResultDto.RiskItem r) {
        StringBuilder sb = new StringBuilder();
        sb.append("风险等级：").append(r.getSeverity() != null ? r.getSeverity() : "").append("\n");
        if (r.getDescription() != null && !r.getDescription().isBlank()) {
            sb.append("风险描述：").append(r.getDescription()).append("\n");
        }
        if (r.getSuggestion() != null && !r.getSuggestion().isBlank()) {
            sb.append("合规建议：").append(r.getSuggestion());
        }
        return sb.toString().trim();
    }

    private static String authorBySeverity(String severity) {
        if (severity == null) return AUTHOR_LOW;
        return switch (severity) {
            case "高" -> AUTHOR_HIGH;
            case "中" -> AUTHOR_MEDIUM;
            default -> AUTHOR_LOW;
        };
    }

    private void ensureCommentsPart(XWPFDocument doc) {
        if (doc.getDocComments() == null) {
            doc.createComments();
        }
    }

    /** Use 1-based comment IDs for better compatibility with Word/OnlyOffice. */
    private BigInteger addCommentToDocument(XWPFDocument doc, String text, String author) {
        XWPFComments comments = doc.getDocComments();
        if (comments == null) comments = doc.createComments();
        int nextId = comments.getComments().size() + 1;
        BigInteger id = BigInteger.valueOf(nextId);
        XWPFComment comment = comments.createComment(id);
        comment.setAuthor(author);
        comment.setDate(Calendar.getInstance());
        if (comment.getParagraphs().isEmpty()) {
            comment.createParagraph();
        }
        XWPFParagraph commentPara = comment.getParagraphs().get(0);
        XWPFRun run = commentPara.createRun();
        run.setText(text);
        return id;
    }

    private static final String W_NS = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";

    private static final QName W_R = new QName(W_NS, "r");

    /**
     * Insert comment range so it wraps only the runs that contain the clause text.
     * Word OOXML: commentRangeStart → (runs containing clause) → commentRangeEnd → commentReference.
     * This avoids highlighting the whole paragraph or full document.
     */
    private void insertCommentRangeInParagraph(XWPFParagraph paragraph, BigInteger commentId, String clause) {
        List<XWPFRun> runs = paragraph.getRuns();
        if (runs.isEmpty()) {
            log.warn("Annotate: paragraph has no runs, comment id={}", commentId);
            return;
        }
        String paraText = paragraph.getText();
        if (paraText == null) paraText = "";
        String normPara = normalizeWhitespace(paraText);
        String normClause = normalizeWhitespace(clause);
        int[] runRange = findRunRangeForClause(runs, normPara, normClause);
        int firstRunIdx = runRange[0];
        int lastRunIdx = runRange[1];

        CTP ctp = paragraph.getCTP();
        // Insert commentRangeStart before the first run of the range
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR firstRun = runs.get(firstRunIdx).getCTR();
        XmlCursor cur = firstRun.newCursor();
        try {
            String startFrag = "<w:commentRangeStart xmlns:w=\"" + W_NS + "\" w:id=\"" + commentId + "\"/>";
            XmlObject startObj = XmlObject.Factory.parse(startFrag);
            XmlCursor startCur = startObj.newCursor();
            try {
                startCur.toFirstChild();
                startCur.moveXml(cur);
            } finally {
                startCur.dispose();
            }
        } catch (Exception e) {
            log.warn("Annotate: insert commentRangeStart failed for id={}: {}", commentId, e.getMessage());
        } finally {
            cur.dispose();
        }

        // Insert commentRangeEnd and commentReference run after the last run of the range (so range wraps clause correctly)
        XmlCursor ctpCur = ctp.newCursor();
        try {
            if (!ctpCur.toFirstChild()) {
                appendCommentRangeEndAndRef(ctp, commentId);
                return;
            }
            int runCount = 0;
            XmlCursor look = ctp.newCursor();
            try {
                look.toFirstChild();
                do {
                    QName name = look.getName();
                    if (name != null && W_NS.equals(name.getNamespaceURI()) && "r".equals(name.getLocalPart())) {
                        runCount++;
                        if (runCount == lastRunIdx + 2) {
                            ctpCur.toCursor(look);
                            break;
                        }
                    }
                } while (look.toNextSibling());
            } finally {
                look.dispose();
            }
            if (runCount < lastRunIdx + 2) {
                appendCommentRangeEndAndRef(ctp, commentId);
                return;
            }
            String endOnlyFrag = "<w:commentRangeEnd xmlns:w=\"" + W_NS + "\" w:id=\"" + commentId + "\"/>";
            String refOnlyFrag = "<w:r xmlns:w=\"" + W_NS + "\"><w:commentReference w:id=\"" + commentId + "\"/><w:t xml:space=\"preserve\">\u200B</w:t></w:r>";
            XmlObject endObj = XmlObject.Factory.parse(endOnlyFrag);
            XmlCursor endFragCur = endObj.newCursor();
            try {
                endFragCur.toFirstChild();
                endFragCur.moveXml(ctpCur);
            } finally {
                endFragCur.dispose();
            }
            XmlObject refObj = XmlObject.Factory.parse(refOnlyFrag);
            XmlCursor refFragCur = refObj.newCursor();
            try {
                refFragCur.toFirstChild();
                refFragCur.moveXml(ctpCur);
            } finally {
                refFragCur.dispose();
            }
        } catch (Exception e) {
            log.warn("Annotate: insert commentRangeEnd failed for id={}, appending at end: {}", commentId, e.getMessage());
            appendCommentRangeEndAndRef(ctp, commentId);
        } finally {
            ctpCur.dispose();
        }
    }

    private static void appendCommentRangeEndAndRef(CTP ctp, BigInteger commentId) {
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTMarkupRange end = ctp.addNewCommentRangeEnd();
        end.setId(commentId);
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR refRun = ctp.addNewR();
        refRun.addNewCommentReference().setId(commentId);
        refRun.addNewT().setStringValue("\u200B");
    }

    /**
     * Find the first and last run indices (inclusive) that together contain the clause.
     * Returns [0, runs.size()-1] if clause cannot be located (fallback to whole paragraph).
     */
    private static int[] findRunRangeForClause(List<XWPFRun> runs, String normPara, String normClause) {
        if (normClause.isBlank()) {
            return new int[] { 0, runs.size() - 1 };
        }
        List<String> runTexts = new ArrayList<>();
        for (XWPFRun r : runs) {
            String t = r.getText(0);
            runTexts.add(t != null ? t : "");
        }
        List<String> normRunTexts = new ArrayList<>();
        for (String t : runTexts) {
            normRunTexts.add(normalizeWhitespace(t));
        }
        StringBuilder sb = new StringBuilder();
        for (String t : normRunTexts) sb.append(t);
        String normFull = sb.toString();
        int start = normFull.indexOf(normClause);
        int matchLen = normClause.length();
        if (start < 0) {
            String sub = normClause.length() > 50 ? normClause.substring(0, 50) : normClause;
            start = normFull.indexOf(sub);
            matchLen = sub.length();
        }
        if (start < 0) {
            return new int[] { 0, runs.size() - 1 };
        }
        int end = start + matchLen;
        if (end > normFull.length()) end = normFull.length();
        int charIdx = 0;
        int firstRunIdx = 0;
        int lastRunIdx = 0;
        for (int i = 0; i < normRunTexts.size(); i++) {
            int runLen = normRunTexts.get(i).length();
            if (charIdx + runLen > start && firstRunIdx == 0) firstRunIdx = i;
            if (charIdx + runLen >= end) {
                lastRunIdx = i;
                break;
            }
            charIdx += runLen;
        }
        if (lastRunIdx < firstRunIdx) lastRunIdx = firstRunIdx;
        return new int[] { firstRunIdx, lastRunIdx };
    }

    private static byte[] toBytes(XWPFDocument doc) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        doc.write(out);
        return out.toByteArray();
    }
}
