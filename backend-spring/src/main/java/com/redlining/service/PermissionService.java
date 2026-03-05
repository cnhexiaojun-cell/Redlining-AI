package com.redlining.service;

import com.redlining.entity.Permission;
import com.redlining.entity.User;
import com.redlining.entity.UserRole;
import com.redlining.repository.PermissionRepository;
import com.redlining.repository.RolePermissionRepository;
import com.redlining.repository.UserRepository;
import com.redlining.repository.UserRoleRepository;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resolves permission codes and data scope for a user (union of all roles).
 * Super admin has all permissions and data:all.
 */
@Service
public class PermissionService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionRepository permissionRepository;

    public PermissionService(UserRepository userRepository,
                             UserRoleRepository userRoleRepository,
                             RolePermissionRepository rolePermissionRepository,
                             PermissionRepository permissionRepository) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.permissionRepository = permissionRepository;
    }

    public boolean isSuperAdmin(User user) {
        return user != null && user.isSuperAdmin();
    }

    /** Returns all permission codes for the user (menu + button + data). Super admin gets all codes. */
    public Set<String> getPermissionCodes(User user) {
        if (user == null) return Set.of();
        if (user.isSuperAdmin()) {
            return permissionRepository.findAll().stream()
                    .map(Permission::getCode)
                    .collect(Collectors.toSet());
        }
        List<Long> roleIds = userRoleRepository.findByUserId(user.getId()).stream()
                .map(UserRole::getRoleId)
                .distinct()
                .toList();
        return getPermissionCodesByRoleIds(roleIds);
    }

    /** Get permission codes from role IDs. */
    public Set<String> getPermissionCodesByRoleIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) return Set.of();
        Set<String> codes = new HashSet<>();
        for (Long roleId : roleIds) {
            for (var rp : rolePermissionRepository.findByRoleId(roleId)) {
                permissionRepository.findById(rp.getPermissionId()).ifPresent(p -> codes.add(p.getCode()));
            }
        }
        return codes;
    }

    public Set<String> getPermissionCodesForUserEntity(User user) {
        if (user == null) return Set.of();
        if (user.isSuperAdmin()) {
            return permissionRepository.findAll().stream()
                    .map(Permission::getCode)
                    .collect(Collectors.toSet());
        }
        List<Long> roleIds = userRoleRepository.findByUserId(user.getId()).stream()
                .map(UserRole::getRoleId)
                .distinct()
                .toList();
        return getPermissionCodesByRoleIds(roleIds);
    }

    /** Data scope: own < dept < all. Returns the strongest scope the user has. */
    public String getDataScope(User user) {
        Set<String> codes = getPermissionCodesForUserEntity(user);
        if (codes.contains("data:all")) return "all";
        if (codes.contains("data:dept")) return "dept";
        if (codes.contains("data:own")) return "own";
        return "own";
    }

    public boolean hasAnyAdminMenuPermission(User user) {
        Set<String> codes = getPermissionCodesForUserEntity(user);
        return codes.stream().anyMatch(c -> c.startsWith("admin:") && c.endsWith(":menu"));
    }
}
