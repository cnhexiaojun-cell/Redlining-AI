package com.redlining.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class RedisConfig {

    private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);

    @Bean
    @ConditionalOnBean(RedisConnectionFactory.class)
    public OptionalRedisTemplate optionalRedisTemplate(RedisConnectionFactory connectionFactory) {
        try {
            StringRedisTemplate template = new StringRedisTemplate(connectionFactory);
            template.afterPropertiesSet();
            return new OptionalRedisTemplate(template);
        } catch (Exception e) {
            log.warn("Redis not available, analysis cache disabled: {}", e.getMessage());
            return new OptionalRedisTemplate();
        }
    }

    @Bean
    @ConditionalOnMissingBean(OptionalRedisTemplate.class)
    public OptionalRedisTemplate noOpRedisTemplate() {
        return new OptionalRedisTemplate();
    }
}
