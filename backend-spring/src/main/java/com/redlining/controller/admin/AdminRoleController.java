package com.redlining.controller.admin;

import com.redlining.dto.admin.RoleCreateRequest;
import com.redlining.dto.admin.RoleDto;
import com.redlining.entity.User;
import com.redlining.service.OperationLogService;
import com.redlining.service.admin.AdminRoleService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/roles")
public class AdminRoleController {

    private final AdminRoleService adminRoleService;
    private final OperationLogService operationLogService;

    public AdminRoleController(AdminRoleService adminRoleService, OperationLogService operationLogService) {
        this.adminRoleService = adminRoleService;
        this.operationLogService = operationLogService;
    }

    @GetMapping
    public Page<RoleDto> list(@PageableDefault(size = 20) Pageable pageable) {
        return adminRoleService.list(pageable);
    }

    @GetMapping("/{id}")
    public RoleDto get(@PathVariable Long id) {
        return adminRoleService.get(id);
    }

    @PostMapping
    public ResponseEntity<RoleDto> create(@AuthenticationPrincipal User user,
                                          @Valid @RequestBody RoleCreateRequest request,
                                          HttpServletRequest req) {
        RoleDto dto = adminRoleService.create(
                request.getName(),
                request.getCode(),
                request.getDescription(),
                request.getPermissionIds());
        operationLogService.log(user, "role", "create", req, request.getCode());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/{id}")
    public RoleDto update(@AuthenticationPrincipal User user,
                          @PathVariable Long id,
                          @Valid @RequestBody RoleCreateRequest request,
                          HttpServletRequest req) {
        RoleDto dto = adminRoleService.update(id,
                request.getName(),
                request.getCode(),
                request.getDescription(),
                request.getPermissionIds());
        operationLogService.log(user, "role", "update", req);
        return dto;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal User user, @PathVariable Long id, HttpServletRequest req) {
        adminRoleService.delete(id);
        operationLogService.log(user, "role", "delete", req);
    }
}
