package com.redlining.controller.admin;

import com.redlining.dto.admin.PlanCreateRequest;
import com.redlining.dto.admin.PlanDto;
import com.redlining.entity.User;
import com.redlining.service.OperationLogService;
import com.redlining.service.admin.AdminPlanService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/plans")
public class AdminPlanController {

    private final AdminPlanService adminPlanService;
    private final OperationLogService operationLogService;

    public AdminPlanController(AdminPlanService adminPlanService, OperationLogService operationLogService) {
        this.adminPlanService = adminPlanService;
        this.operationLogService = operationLogService;
    }

    @GetMapping
    public List<PlanDto> list() {
        return adminPlanService.list();
    }

    @GetMapping("/{id}")
    public PlanDto get(@PathVariable Long id) {
        return adminPlanService.get(id);
    }

    @PostMapping
    public ResponseEntity<PlanDto> create(@AuthenticationPrincipal User user,
                                          @Valid @RequestBody PlanCreateRequest request,
                                          HttpServletRequest req) {
        PlanDto dto = adminPlanService.create(request);
        operationLogService.log(user, "plan", "create", req, request.getCode());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/{id}")
    public PlanDto update(@AuthenticationPrincipal User user,
                         @PathVariable Long id,
                         @Valid @RequestBody PlanCreateRequest request,
                         HttpServletRequest req) {
        PlanDto dto = adminPlanService.update(id, request);
        operationLogService.log(user, "plan", "update", req);
        return dto;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal User user, @PathVariable Long id, HttpServletRequest req) {
        adminPlanService.delete(id);
        operationLogService.log(user, "plan", "delete", req);
    }
}
