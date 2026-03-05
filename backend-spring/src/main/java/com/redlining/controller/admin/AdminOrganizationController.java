package com.redlining.controller.admin;

import com.redlining.dto.admin.OrganizationCreateRequest;
import com.redlining.dto.admin.OrganizationDto;
import com.redlining.service.OperationLogService;
import com.redlining.service.admin.AdminOrganizationService;
import com.redlining.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/organizations")
public class AdminOrganizationController {

    private final AdminOrganizationService adminOrganizationService;
    private final OperationLogService operationLogService;

    public AdminOrganizationController(AdminOrganizationService adminOrganizationService, OperationLogService operationLogService) {
        this.adminOrganizationService = adminOrganizationService;
        this.operationLogService = operationLogService;
    }

    @GetMapping("/tree")
    public List<OrganizationDto> getTree(@RequestParam(required = false) Long parentId) {
        return adminOrganizationService.getTree(parentId);
    }

    @GetMapping
    public List<OrganizationDto> list() {
        return adminOrganizationService.getFlatList();
    }

    @PostMapping
    public ResponseEntity<OrganizationDto> create(@AuthenticationPrincipal User user,
                                                   @Valid @RequestBody OrganizationCreateRequest request,
                                                   HttpServletRequest req) {
        OrganizationDto dto = adminOrganizationService.create(request);
        operationLogService.log(user, "organization", "create", req, request.getCode());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/{id}")
    public OrganizationDto update(@AuthenticationPrincipal User user,
                                  @PathVariable Long id,
                                  @Valid @RequestBody OrganizationCreateRequest request,
                                  HttpServletRequest req) {
        OrganizationDto dto = adminOrganizationService.update(id, request);
        operationLogService.log(user, "organization", "update", req);
        return dto;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal User user, @PathVariable Long id, HttpServletRequest req) {
        adminOrganizationService.delete(id);
        operationLogService.log(user, "organization", "delete", req);
    }
}
