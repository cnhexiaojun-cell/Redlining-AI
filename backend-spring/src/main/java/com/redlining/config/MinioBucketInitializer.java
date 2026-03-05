package com.redlining.config;

import com.redlining.service.MinioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class MinioBucketInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MinioBucketInitializer.class);

    private final MinioService minioService;

    public MinioBucketInitializer(MinioService minioService) {
        this.minioService = minioService;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            minioService.ensureBucket();
        } catch (Exception e) {
            log.warn("MinIO bucket init skipped (MinIO may be unavailable): {}", e.getMessage());
        }
    }
}
