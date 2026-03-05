package com.redlining.controller.admin;

import com.redlining.dto.admin.ResetPasswordRequest;
import com.redlining.dto.admin.UserAdminDto;
import com.redlining.dto.admin.UserCreateRequest;
import com.redlining.dto.admin.UserUpdateRequest;
import com.redlining.entity.User;
import com.redlining.service.OperationLogService;
import com.redlining.service.admin.AdminUserService;
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
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final OperationLogService operationLogService;

    public AdminUserController(AdminUserService adminUserService, OperationLogService operationLogService) {
        this.adminUserService = adminUserService;
        this.operationLogService = operationLogService;
    }

    @GetMapping
    public Page<UserAdminDto> list(@RequestParam(required = false) Long organizationId,
                                   @RequestParam(required = false) Boolean enabled,
                                   @PageableDefault(size = 20) Pageable pageable) {
        return adminUserService.list(organizationId, enabled, pageable);
    }

    @GetMapping("/{id}")
    public UserAdminDto get(@PathVariable Long id) {
        return adminUserService.get(id);
    }

    @PostMapping
    public ResponseEntity<UserAdminDto> create(@AuthenticationPrincipal User user,
                                                @Valid @RequestBody UserCreateRequest request,
                                                HttpServletRequest req) {
        UserAdminDto dto = adminUserService.create(
                request.getUsername(),
                request.getEmail(),
                request.getPassword(),
                request.getRealName(),
                request.getOrganizationId(),
                request.getRoleIds());
        operationLogService.log(user, "user", "create", req, request.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/{id}")
    public UserAdminDto update(@AuthenticationPrincipal User user,
                               @PathVariable Long id,
                               @Valid @RequestBody UserUpdateRequest request,
                               HttpServletRequest req) {
        UserAdminDto dto = adminUserService.update(
                id,
                request.getEmail(),
                request.getRealName(),
                request.getEnabled(),
                request.getOrganizationId(),
                request.getRoleIds());
        operationLogService.log(user, "user", "update", req);
        return dto;
    }

    @PostMapping("/{id}/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@AuthenticationPrincipal User user,
                              @PathVariable Long id,
                              @Valid @RequestBody ResetPasswordRequest request,
                              HttpServletRequest req) {
        adminUserService.resetPassword(id, request.getPassword());
        operationLogService.log(user, "user", "resetPassword", req);
    }
}
