package com.redlining.config;

import com.redlining.service.CaptchaServiceInterface;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Test 环境下使用固定验证码 "TEST"，便于集成测试。
 */
@Configuration
public class TestCaptchaConfig {

    private static final String FIXED_CODE = "TEST";
    private final Map<String, String> store = new ConcurrentHashMap<>();

    @Bean
    @Primary
    public CaptchaServiceInterface testCaptchaService() {
        return new CaptchaServiceInterface() {
            @Override
            public Map<String, String> createCaptcha() {
                String captchaId = UUID.randomUUID().toString();
                store.put(captchaId, FIXED_CODE);
                return Map.of(
                        "captchaId", captchaId,
                        "image", "data:image/png;base64,test"
                );
            }

            @Override
            public boolean validate(String captchaId, String userInput) {
                String code = store.remove(captchaId);
                return code != null && code.equalsIgnoreCase(userInput != null ? userInput.trim() : "");
            }
        };
    }
}
