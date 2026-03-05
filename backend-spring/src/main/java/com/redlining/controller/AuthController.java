package com.redlining.controller;

import com.redlining.dto.LoginRequest;
import com.redlining.dto.LoginResponse;
import com.redlining.dto.PermissionsResponse;
import com.redlining.dto.RegisterRequest;
import com.redlining.dto.UpdateProfileRequest;
import com.redlining.dto.UserResponse;
import com.redlining.entity.User;
import com.redlining.service.AuthService;
import com.redlining.service.CaptchaServiceInterface;
import com.redlining.service.PermissionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthService authService;
    private final CaptchaServiceInterface captchaService;
    private final PermissionService permissionService;

    public AuthController(AuthService authService, CaptchaServiceInterface captchaService, PermissionService permissionService) {
        this.authService = authService;
        this.captchaService = captchaService;
        this.permissionService = permissionService;
    }

    @GetMapping("/captcha")
    public ResponseEntity<Map<String, String>> captcha() {
        return ResponseEntity.ok(captchaService.createCaptcha());
    }

    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(
                request.getUsername(),
                request.getPassword(),
                request.getCaptchaId(),
                request.getCaptchaCode()));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(authService.getProfileResponse(user));
    }

    @GetMapping("/me/permissions")
    public ResponseEntity<PermissionsResponse> permissions(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Set<String> all = permissionService.getPermissionCodesForUserEntity(user);
        Set<String> menuCodes = all.stream().filter(c -> c.endsWith(":menu")).collect(Collectors.toSet());
        Set<String> buttonCodes = all.stream().filter(c -> c.endsWith(":button") || c.startsWith("admin:") && !c.endsWith(":menu") && !c.startsWith("data:")).collect(Collectors.toSet());
        String dataScope = permissionService.getDataScope(user);
        PermissionsResponse res = new PermissionsResponse(permissionService.isSuperAdmin(user), menuCodes, buttonCodes, dataScope);
        return ResponseEntity.ok(res);
    }

    @PatchMapping("/me")
    public ResponseEntity<UserResponse> updateProfile(@AuthenticationPrincipal User user,
                                                      @Valid @RequestBody UpdateProfileRequest request) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(authService.updateProfile(user.getId(), request));
    }

    @PostMapping("/me/avatar")
    public ResponseEntity<UserResponse> uploadAvatar(@AuthenticationPrincipal User user,
                                                      @RequestParam("file") MultipartFile file) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(authService.updateAvatar(user.getId(), file));
    }
}
