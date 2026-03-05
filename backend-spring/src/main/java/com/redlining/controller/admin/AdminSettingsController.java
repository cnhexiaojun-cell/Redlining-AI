package com.redlining.controller.admin;

import com.redlining.dto.admin.SystemSettingDto;
import com.redlining.entity.User;
import com.redlining.service.OperationLogService;
import com.redlining.service.admin.AdminSettingsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/settings")
public class AdminSettingsController {

    private final AdminSettingsService adminSettingsService;
    private final OperationLogService operationLogService;

    public AdminSettingsController(AdminSettingsService adminSettingsService, OperationLogService operationLogService) {
        this.adminSettingsService = adminSettingsService;
        this.operationLogService = operationLogService;
    }

    @GetMapping
    public List<SystemSettingDto> list() {
        return adminSettingsService.list();
    }

    @PutMapping
    public void updateBatch(@AuthenticationPrincipal User user,
                            @RequestBody Map<String, String> updates,
                            HttpServletRequest req) {
        adminSettingsService.updateBatch(updates);
        operationLogService.log(user, "settings", "update", req);
    }
}
