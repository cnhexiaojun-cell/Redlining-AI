package com.redlining.security;

import com.redlining.entity.User;
import com.redlining.service.PermissionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * For /api/admin/** requests: allow only if user is super admin or has at least one admin menu permission.
 */
@Component
public class AdminAccessFilter extends OncePerRequestFilter {

    private final PermissionService permissionService;

    public AdminAccessFilter(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path == null || !path.startsWith("/api/admin");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof User)) {
            filterChain.doFilter(request, response);
            return;
        }
        User user = (User) auth.getPrincipal();
        if (permissionService.isSuperAdmin(user) || permissionService.hasAnyAdminMenuPermission(user)) {
            filterChain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"error\":\"Forbidden\",\"message\":\"No admin access\"}");
    }
}
