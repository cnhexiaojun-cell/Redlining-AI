package com.redlining.service.admin;

import com.redlining.dto.admin.SystemSettingDto;
import com.redlining.entity.SystemSetting;
import com.redlining.repository.SystemSettingRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class AdminSettingsService {

    private static final List<String> ALLOWED_KEYS = List.of(
            "onlyoffice.enabled", "upload.maxSize", "feature.xxx"
    );

    private final SystemSettingRepository systemSettingRepository;

    public AdminSettingsService(SystemSettingRepository systemSettingRepository) {
        this.systemSettingRepository = systemSettingRepository;
    }

    public List<SystemSettingDto> list() {
        return StreamSupport.stream(systemSettingRepository.findAll().spliterator(), false)
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateBatch(Map<String, String> updates) {
        if (updates == null) return;
        for (Map.Entry<String, String> e : updates.entrySet()) {
            String key = e.getKey();
            if (key == null || key.isBlank()) continue;
            if (!ALLOWED_KEYS.contains(key)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不允许修改: " + key);
            }
            SystemSetting s = systemSettingRepository.findBySettingKey(key).orElseGet(() -> {
                SystemSetting n = new SystemSetting();
                n.setSettingKey(key);
                return n;
            });
            s.setValue(e.getValue());
            s.setUpdatedAt(Instant.now());
            systemSettingRepository.save(s);
        }
    }

    private SystemSettingDto toDto(SystemSetting s) {
        SystemSettingDto dto = new SystemSettingDto();
        dto.setId(s.getId());
        dto.setKey(s.getSettingKey());
        dto.setValue(s.getValue());
        dto.setDescription(s.getDescription());
        dto.setValueType(s.getValueType());
        dto.setUpdatedAt(s.getUpdatedAt());
        return dto;
    }
}
