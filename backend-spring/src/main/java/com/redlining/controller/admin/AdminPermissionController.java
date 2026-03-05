package com.redlining.controller.admin;

import com.redlining.dto.admin.PermissionTreeDto;
import com.redlining.service.admin.AdminPermissionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/permissions")
public class AdminPermissionController {

    private final AdminPermissionService adminPermissionService;

    public AdminPermissionController(AdminPermissionService adminPermissionService) {
        this.adminPermissionService = adminPermissionService;
    }

    @GetMapping("/tree")
    public List<PermissionTreeDto> getTree() {
        return adminPermissionService.getTree();
    }
}
