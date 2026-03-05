package com.redlining.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redlining.config.AppProperties;
import com.redlining.config.OptionalRedisTemplate;
import com.redlining.dto.AnalysisResultDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
public class AnalysisCacheService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisCacheService.class);
    private static final String KEY_PREFIX = "redlining:analysis:";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final AppProperties appProperties;
    private final OptionalRedisTemplate optionalRedis;

    public AnalysisCacheService(AppProperties appProperties, OptionalRedisTemplate optionalRedis) {
        this.appProperties = appProperties;
        this.optionalRedis = optionalRedis;
    }

    public String buildKey(String contractText, String stance, String advancedRules) {
        String input = (contractText != null ? contractText : "")
                + "|" + (stance != null ? stance : "")
                + "|" + (advancedRules != null ? advancedRules : "");
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return KEY_PREFIX + HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            return KEY_PREFIX + Integer.toHexString(input.hashCode());
        }
    }

    public AnalysisResultDto get(String key) {
        if (!optionalRedis.isAvailable()) {
            return null;
        }
        try {
            String json = optionalRedis.get(key);
            if (json == null || json.isBlank()) {
                return null;
            }
            return objectMapper.readValue(json, AnalysisResultDto.class);
        } catch (Exception e) {
            log.debug("Cache get failed: {}", e.getMessage());
            return null;
        }
    }

    public void put(String key, AnalysisResultDto result) {
        if (!optionalRedis.isAvailable()) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(result);
            long ttl = appProperties.getCache().getAnalysisTtlSeconds();
            optionalRedis.set(key, json, ttl);
        } catch (JsonProcessingException e) {
            log.debug("Cache put failed: {}", e.getMessage());
        }
    }
}
