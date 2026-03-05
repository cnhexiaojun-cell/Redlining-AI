package com.redlining.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redlining.config.AppProperties;
import com.redlining.dto.AnalysisResultDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ContractAnalyzerService {

    private static final Logger log = LoggerFactory.getLogger(ContractAnalyzerService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final AppProperties appProperties;
    private final WebClient.Builder webClientBuilder;
    private final AnalysisCacheService cacheService;

    public ContractAnalyzerService(AppProperties appProperties,
                                   WebClient.Builder webClientBuilder,
                                   AnalysisCacheService cacheService) {
        this.appProperties = appProperties;
        this.webClientBuilder = webClientBuilder;
        this.cacheService = cacheService;
    }

    public AnalysisResultDto analyze(String contractText, String stance, String advancedRules) {
        String cacheKey = cacheService.buildKey(contractText, stance, advancedRules);
        AnalysisResultDto cached = cacheService.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        String apiKey = appProperties.getLlm().getApiKey();
        String baseUrl = appProperties.getLlm().getBaseUrl();
        if (apiKey == null || apiKey.isEmpty() || "your_api_key_here".equals(apiKey)) {
            log.warn("DeepSeek NOT called: API key not configured; returning fallback.");
            return createFallback(contractText, true, null);
        }

        String stanceHint = switch (stance == null ? "" : stance) {
            case "party-a" -> "\n审查立场：甲方（买方/雇主），请优先从甲方利益角度识别风险与不利条款。";
            case "party-b" -> "\n审查立场：乙方（卖方/雇员），请优先从乙方利益角度识别风险与不利条款。";
            case "neutral" -> "\n审查立场：中立，请公平识别对双方可能不利的条款。";
            default -> "";
        };
        String rulesHint = (advancedRules != null && !advancedRules.isBlank())
                ? "\n用户额外关注或规则：" + advancedRules.strip()
                : "";

        String prompt = "作为专业合同审查专家，请分析以下合同并返回严格JSON格式：\n"
                + contractText + stanceHint + rulesHint + "\n\n"
                + "必须返回以下JSON格式（不要添加markdown标记）：\n"
                + "{\n  \"summary\": \"合同类型和金额概述\",\n  \"risks\": [{\"type\": \"...\", \"description\": \"...\", \"severity\": \"高/中/低\", \"clause\": \"...\", \"suggestion\": \"...\"}],\n"
                + "  \"missing_clauses\": [{\"clause\": \"...\", \"importance\": \"...\", \"recommendation\": \"...\"}],\n  \"compliance_score\": 75,\n  \"key_points\": [\"要点1\", \"要点2\"]\n}";

        Map<String, Object> body = Map.of(
                "model", appProperties.getLlm().getModelName(),
                "messages", List.of(
                        Map.of("role", "system", "content", "你是专业合同审查专家，只返回纯JSON格式，确保完整不截断。"),
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.1,
                "max_tokens", 2500
        );

        log.info("Calling DeepSeek for analysis, contentLength={}, stance={}", contractText.length(), stance);
        String failureNotice = "AI 分析暂时不可用，当前为参考结果，请稍后重试。";
        try {
            String responseBody = webClientBuilder.build()
                    .post()
                    .uri(baseUrl + "/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(120));
            if (responseBody == null) {
                log.warn("DeepSeek NOT used: response body null; returning fallback.");
                return createFallback(contractText, false, failureNotice);
            }
            Map<?, ?> parsed = objectMapper.readValue(responseBody, Map.class);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) parsed.get("choices");
            if (choices == null || choices.isEmpty()) {
                log.warn("DeepSeek NOT used: choices empty; returning fallback.");
                return createFallback(contractText, false, failureNotice);
            }
            Map<String, Object> first = choices.get(0);
            Map<String, Object> message = (Map<String, Object>) first.get("message");
            String content = message != null ? String.valueOf(message.get("content")) : "";
            AnalysisResultDto result = smartExtractJson(content);
            if (result != null) {
                cacheService.put(cacheKey, result);
                log.info("DeepSeek analysis succeeded, result cached.");
                return result;
            }
            log.warn("DeepSeek NOT used: JSON parse failed (content length={}); returning fallback.", content.length());
            if (log.isDebugEnabled()) {
                log.debug("DeepSeek response content preview: {}", content.length() > 200 ? content.substring(0, 200) + "…" : content);
            }
            failureNotice = "AI 分析暂时不可用：返回格式解析失败，当前为参考结果。请稍后重试或联系管理员。";
            return createFallback(contractText, false, failureNotice);
        } catch (WebClientResponseException e) {
            int code = e.getStatusCode().value();
            log.warn("DeepSeek request failed: status={}, body={}; returning fallback.", e.getStatusCode(), e.getResponseBodyAsString());
            if (code == 401) {
                failureNotice = "AI 分析暂时不可用：API 密钥无效或已过期，请检查 DEEPSEEK_API_KEY 后重启后端。";
            } else if (code == 402) {
                failureNotice = "AI 分析暂时不可用：账户余额不足或需要充值（402），请前往 DeepSeek 控制台充值后重试。";
            } else if (code == 429) {
                failureNotice = "AI 分析暂时不可用：请求过于频繁，请稍后重试。";
            } else if (code >= 500) {
                failureNotice = "AI 分析暂时不可用：DeepSeek 服务异常（" + code + "），请稍后重试。";
            } else {
                failureNotice = "AI 分析暂时不可用：API 返回 " + code + "，当前为参考结果。请稍后重试。";
            }
            return createFallback(contractText, false, failureNotice);
        } catch (Exception e) {
            log.error("Analysis failed", e);
            log.warn("DeepSeek NOT used: exception; returning fallback.");
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.contains("Connection") || msg.contains("timeout") || msg.contains("Timeout")) {
                failureNotice = "AI 分析暂时不可用：网络超时或连接失败，请检查网络后重试。";
            } else {
                failureNotice = "AI 分析暂时不可用，当前为参考结果，请稍后重试。";
            }
            return createFallback(contractText, false, failureNotice);
        }
    }

    @SuppressWarnings("unchecked")
    private AnalysisResultDto smartExtractJson(String text) {
        try {
            text = text.strip().replaceAll("```(?:json)?\\s*|\\s*```", "");
            try {
                Map<String, Object> map = objectMapper.readValue(text, Map.class);
                return mapToDto(map);
            } catch (Exception ignored) {
            }
            Pattern p = Pattern.compile("\\{[^{}]*(?:\\{[^{}]*\\}[^{}]*)*}");
            Matcher m = p.matcher(text);
            while (m.find()) {
                try {
                    Map<String, Object> map = objectMapper.readValue(m.group(), Map.class);
                    if (map.containsKey("summary") && map.containsKey("risks")) {
                        return mapToDto(map);
                    }
                } catch (Exception ignored) {
                }
            }
            text = text.replaceAll(",(\\s*[}\\]])", "$1");
            Map<String, Object> map = objectMapper.readValue(text, Map.class);
            return mapToDto(map);
        } catch (Exception e) {
            log.debug("JSON extraction failed: {}", e.getMessage());
            return null;
        }
    }

    private AnalysisResultDto mapToDto(Map<String, Object> map) {
        AnalysisResultDto dto = new AnalysisResultDto();
        dto.setSummary(map.get("summary") != null ? map.get("summary").toString() : "");
        dto.setCompliance_score(map.get("compliance_score") instanceof Number n ? n.intValue() : 75);
        if (map.get("risks") instanceof List<?> risks) {
            dto.setRisks(risks.stream()
                    .filter(r -> r instanceof Map)
                    .map(r -> toRiskItem((Map<?, ?>) r))
                    .limit(5)
                    .toList());
        }
        if (map.get("missing_clauses") instanceof List<?> missing) {
            dto.setMissing_clauses(missing.stream()
                    .filter(m -> m instanceof Map)
                    .map(m -> toMissingClause((Map<?, ?>) m))
                    .toList());
        }
        if (map.get("key_points") instanceof List<?> kp) {
            dto.setKey_points(kp.stream().map(Object::toString).toList());
        }
        return dto;
    }

    private AnalysisResultDto.RiskItem toRiskItem(Map<?, ?> m) {
        AnalysisResultDto.RiskItem item = new AnalysisResultDto.RiskItem();
        item.setType(getStr(m, "type"));
        item.setDescription(getStr(m, "description"));
        item.setSeverity(getStr(m, "severity"));
        item.setClause(getStr(m, "clause"));
        item.setSuggestion(getStr(m, "suggestion"));
        return item;
    }

    private AnalysisResultDto.MissingClause toMissingClause(Map<?, ?> m) {
        AnalysisResultDto.MissingClause item = new AnalysisResultDto.MissingClause();
        item.setClause(getStr(m, "clause"));
        item.setImportance(getStr(m, "importance"));
        item.setRecommendation(getStr(m, "recommendation"));
        return item;
    }

    private static String getStr(Map<?, ?> m, String key) {
        Object v = m.get(key);
        return v == null ? "" : v.toString();
    }

    private AnalysisResultDto createFallback(String rawText, boolean isPlaceholder, String llmFailureNotice) {
        int len = rawText == null ? 0 : rawText.length();
        String preview = (rawText != null && rawText.length() > 80)
                ? rawText.substring(0, 80).replaceAll("\\s+", " ").trim() + "…"
                : (rawText != null ? rawText.replaceAll("\\s+", " ").trim() : "");

        AnalysisResultDto dto = new AnalysisResultDto();
        if (isPlaceholder) {
            dto.setNotice("未配置 AI 密钥，当前为基于合同内容的示例结果。请配置 DEEPSEEK_API_KEY 后获得真实 AI 审查。");
            dto.setSummary("【示例】未配置 AI：已读取合同全文（共 " + len + " 字），以下为根据关键词生成的参考。");
            dto.setKey_points(List.of(
                    "合同已读取，长度: " + len + " 字",
                    "合同开头摘要: " + (preview.isEmpty() ? "（无文本）" : preview),
                    "配置 API 密钥后可获得真实 AI 审查结果"
            ));
        } else {
            if (llmFailureNotice != null && !llmFailureNotice.isEmpty()) {
                dto.setNotice(llmFailureNotice);
            }
            dto.setSummary("合同专业分析完成");
            dto.setKey_points(List.of("合同已审阅", "风险已识别"));
        }
        dto.setCompliance_score(75);

        List<AnalysisResultDto.RiskItem> risks = new ArrayList<>();
        if (rawText != null && rawText.contains("违约")) {
            AnalysisResultDto.RiskItem r = new AnalysisResultDto.RiskItem();
            r.setType("违约责任");
            r.setDescription("合同中违约责任条款需要明确");
            r.setSeverity("中");
            r.setClause("违约条款");
            r.setSuggestion("明确违约金计算方式和上限");
            risks.add(r);
        }
        if (rawText == null || !rawText.contains("知识产权")) {
            AnalysisResultDto.RiskItem r = new AnalysisResultDto.RiskItem();
            r.setType("知识产权风险");
            r.setDescription("缺少知识产权归属条款");
            r.setSeverity("高");
            r.setClause("缺失条款");
            r.setSuggestion("增加软件著作权归属约定");
            risks.add(r);
        }
        if (risks.isEmpty()) {
            AnalysisResultDto.RiskItem r = new AnalysisResultDto.RiskItem();
            r.setType("通用审查");
            r.setDescription("合同需要专业法律审查");
            r.setSeverity("中");
            r.setClause("整体");
            r.setSuggestion("建议专业律师审核");
            risks.add(r);
        }
        dto.setRisks(risks.size() > 5 ? risks.subList(0, 5) : risks);

        List<AnalysisResultDto.MissingClause> missing = new ArrayList<>();
        if (rawText == null || !rawText.contains("知识产权")) {
            AnalysisResultDto.MissingClause mc = new AnalysisResultDto.MissingClause();
            mc.setClause("知识产权条款");
            mc.setImportance("高");
            mc.setRecommendation("明确软件著作权归属");
            missing.add(mc);
        }
        dto.setMissing_clauses(missing);
        return dto;
    }
}
