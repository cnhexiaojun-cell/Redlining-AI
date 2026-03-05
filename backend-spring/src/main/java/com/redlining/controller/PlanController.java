package com.redlining.controller;

import com.redlining.dto.admin.PlanDto;
import com.redlining.entity.User;
import com.redlining.service.admin.AdminPlanService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * User-facing plan list for plan center (no admin permission required).
 */
@RestController
@RequestMapping("/api/plans")
public class PlanController {

    private final AdminPlanService adminPlanService;

    public PlanController(AdminPlanService adminPlanService) {
        this.adminPlanService = adminPlanService;
    }

    @GetMapping
    public List<PlanDto> list(@AuthenticationPrincipal User user) {
        if (user == null) {
            return List.of();
        }
        return adminPlanService.list();
    }
}
