package com.redlining.config;

import com.redlining.entity.Permission;
import com.redlining.repository.PermissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Seeds default permissions (menu, button, data) if the permissions table is empty.
 */
@Component
@Order(100)
public class PermissionSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PermissionSeeder.class);

    private final PermissionRepository permissionRepository;

    public PermissionSeeder(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (permissionRepository.count() > 0) {
            ensurePlanPermissions();
            repairParentIds();
            return;
        }
        log.info("Seeding default permissions");
        List<Permission> list = List.of(
                perm("upload:menu", "上传合同", "menu", null, 10),
                perm("scanning:menu", "扫描中", "menu", null, 20),
                perm("report:menu", "报告", "menu", null, 30),
                perm("documents:menu", "文档管理", "menu", null, 40),
                perm("profile:menu", "个人中心", "menu", null, 50),
                perm("admin:menu", "系统管理", "menu", null, 100),
                perm("admin:organizations:menu", "机构管理", "menu", "admin:menu", 110),
                perm("admin:users:menu", "用户管理", "menu", "admin:menu", 120),
                perm("admin:roles:menu", "角色管理", "menu", "admin:menu", 130),
                perm("admin:logs:menu", "操作日志", "menu", "admin:menu", 140),
                perm("admin:settings:menu", "系统设置", "menu", "admin:menu", 150),
                perm("admin:plans:menu", "套餐管理", "menu", "admin:menu", 155),
                perm("admin:organizations:create", "机构-创建", "button", null, 201),
                perm("admin:organizations:edit", "机构-编辑", "button", null, 202),
                perm("admin:organizations:delete", "机构-删除", "button", null, 203),
                perm("admin:users:create", "用户-创建", "button", null, 211),
                perm("admin:users:edit", "用户-编辑", "button", null, 212),
                perm("admin:users:delete", "用户-删除", "button", null, 213),
                perm("admin:users:resetPassword", "用户-重置密码", "button", null, 214),
                perm("admin:roles:create", "角色-创建", "button", null, 221),
                perm("admin:roles:edit", "角色-编辑", "button", null, 222),
                perm("admin:roles:delete", "角色-删除", "button", null, 223),
                perm("admin:logs:export", "日志-导出", "button", null, 231),
                perm("admin:settings:edit", "系统设置-保存", "button", null, 241),
                perm("admin:plans:create", "套餐-创建", "button", null, 251),
                perm("admin:plans:edit", "套餐-编辑", "button", null, 252),
                perm("admin:plans:delete", "套餐-删除", "button", null, 253),
                perm("data:own", "数据权限-仅本人", "data", null, 301),
                perm("data:dept", "数据权限-本部门", "data", null, 302),
                perm("data:all", "数据权限-全部", "data", null, 303)
        );
        Map<String, Long> codeToId = new java.util.HashMap<>();
        for (Permission p : list) {
            p = permissionRepository.save(p);
            codeToId.put(p.getCode(), p.getId());
        }
        for (Permission p : list) {
            String parentCode = parentCodeByCode.get(p.getCode());
            if (parentCode != null) {
                Long parentId = codeToId.get(parentCode);
                if (parentId != null) {
                    p.setParentId(parentId);
                    permissionRepository.save(p);
                }
            }
        }
        log.info("Seeded {} permissions", list.size());
    }

    /** Add plan permissions if missing (e.g. after upgrade). */
    private void ensurePlanPermissions() {
        for (String code : List.of("admin:plans:menu", "admin:plans:create", "admin:plans:edit", "admin:plans:delete")) {
            if (permissionRepository.findByCode(code).isEmpty()) {
                Permission p = new Permission();
                p.setCode(code);
                p.setName(code.contains("menu") ? "套餐管理" : (code.contains("create") ? "套餐-创建" : code.contains("edit") ? "套餐-编辑" : "套餐-删除"));
                p.setType(code.contains("menu") ? "menu" : "button");
                p.setSortOrder(code.contains("menu") ? 155 : (code.contains("create") ? 251 : code.contains("edit") ? 252 : 253));
                permissionRepository.save(p);
            }
        }
    }

    /** Update parentId for existing permissions so buttons sit under their menu. */
    private void repairParentIds() {
        Map<String, Long> codeToId = permissionRepository.findAll().stream()
                .collect(java.util.stream.Collectors.toMap(Permission::getCode, Permission::getId));
        for (String code : parentCodeByCode.keySet()) {
            String parentCode = parentCodeByCode.get(code);
            Long parentId = codeToId.get(parentCode);
            if (parentId == null) continue;
            permissionRepository.findByCode(code).ifPresent(p -> {
                if (!parentId.equals(p.getParentId())) {
                    p.setParentId(parentId);
                    permissionRepository.save(p);
                }
            });
        }
    }

    /** Menu under 系统管理; button/data under corresponding menu. */
    private static final Map<String, String> parentCodeByCode = Map.ofEntries(
            Map.entry("admin:organizations:menu", "admin:menu"),
            Map.entry("admin:users:menu", "admin:menu"),
            Map.entry("admin:roles:menu", "admin:menu"),
            Map.entry("admin:logs:menu", "admin:menu"),
            Map.entry("admin:settings:menu", "admin:menu"),
            Map.entry("admin:plans:menu", "admin:menu"),
            Map.entry("admin:plans:create", "admin:plans:menu"),
            Map.entry("admin:plans:edit", "admin:plans:menu"),
            Map.entry("admin:plans:delete", "admin:plans:menu"),
            Map.entry("admin:organizations:create", "admin:organizations:menu"),
            Map.entry("admin:organizations:edit", "admin:organizations:menu"),
            Map.entry("admin:organizations:delete", "admin:organizations:menu"),
            Map.entry("admin:users:create", "admin:users:menu"),
            Map.entry("admin:users:edit", "admin:users:menu"),
            Map.entry("admin:users:delete", "admin:users:menu"),
            Map.entry("admin:users:resetPassword", "admin:users:menu"),
            Map.entry("admin:roles:create", "admin:roles:menu"),
            Map.entry("admin:roles:edit", "admin:roles:menu"),
            Map.entry("admin:roles:delete", "admin:roles:menu"),
            Map.entry("admin:logs:export", "admin:logs:menu"),
            Map.entry("admin:settings:edit", "admin:settings:menu")
    );

    private static Permission perm(String code, String name, String type, String parentCode, int sortOrder) {
        Permission p = new Permission();
        p.setCode(code);
        p.setName(name);
        p.setType(type);
        p.setSortOrder(sortOrder);
        return p;
    }
}
