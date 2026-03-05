package com.redlining.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

public class OptionalRedisTemplate {

    private static final Logger log = LoggerFactory.getLogger(OptionalRedisTemplate.class);

    private final StringRedisTemplate template;
    private volatile Boolean available;

    public OptionalRedisTemplate(StringRedisTemplate template) {
        this.template = template;
    }

    public OptionalRedisTemplate() {
        this.template = null;
    }

    public boolean isAvailable() {
        if (template == null) {
            return false;
        }
        if (available != null) {
            return available;
        }
        try {
            template.getConnectionFactory().getConnection().ping();
            available = true;
        } catch (Exception e) {
            log.debug("Redis ping failed: {}", e.getMessage());
            available = false;
        }
        return available;
    }

    public String get(String key) {
        if (template == null) {
            return null;
        }
        try {
            ValueOperations<String, String> ops = template.opsForValue();
            return ops.get(key);
        } catch (Exception e) {
            return null;
        }
    }

    public void set(String key, String value, long ttlSeconds) {
        if (template == null) {
            return;
        }
        try {
            ValueOperations<String, String> ops = template.opsForValue();
            ops.set(key, value, Duration.ofSeconds(ttlSeconds));
        } catch (Exception e) {
            log.debug("Redis set failed: {}", e.getMessage());
        }
    }
}
