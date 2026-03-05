package com.redlining.service.admin;

import com.redlining.dto.admin.RoleDto;
import com.redlining.entity.Role;
import com.redlining.repository.RolePermissionRepository;
import com.redlining.repository.RoleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminRoleService {

    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;

    public AdminRoleService(RoleRepository roleRepository, RolePermissionRepository rolePermissionRepository) {
        this.roleRepository = roleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
    }

    public Page<RoleDto> list(Pageable pageable) {
        return roleRepository.findAll(pageable).map(this::toDto);
    }

    public RoleDto get(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "角色不存在"));
        RoleDto dto = toDto(role);
        dto.setPermissionIds(rolePermissionRepository.findByRoleId(id).stream()
                .map(rp -> rp.getPermissionId())
                .collect(Collectors.toList()));
        return dto;
    }

    @Transactional
    public RoleDto create(String name, String code, String description, List<Long> permissionIds) {
        if (roleRepository.existsByCode(code)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "角色编码已存在");
        }
        Role role = new Role();
        role.setName(name);
        role.setCode(code);
        role.setDescription(description);
        role = roleRepository.save(role);
        if (permissionIds != null) {
            for (Long pid : permissionIds) {
                var rp = new com.redlining.entity.RolePermission();
                rp.setRoleId(role.getId());
                rp.setPermissionId(pid);
                rolePermissionRepository.save(rp);
            }
        }
        return get(role.getId());
    }

    @Transactional
    public RoleDto update(Long id, String name, String code, String description, List<Long> permissionIds) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "角色不存在"));
        if (roleRepository.existsByCodeAndIdNot(code, id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "角色编码已存在");
        }
        role.setName(name);
        role.setCode(code);
        role.setDescription(description);
        roleRepository.save(role);
        rolePermissionRepository.deleteByRoleId(id);
        if (permissionIds != null) {
            for (Long pid : permissionIds) {
                var rp = new com.redlining.entity.RolePermission();
                rp.setRoleId(id);
                rp.setPermissionId(pid);
                rolePermissionRepository.save(rp);
            }
        }
        return get(id);
    }

    @Transactional
    public void delete(Long id) {
        if (!roleRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "角色不存在");
        }
        rolePermissionRepository.deleteByRoleId(id);
        roleRepository.deleteById(id);
    }

    private RoleDto toDto(Role r) {
        RoleDto dto = new RoleDto();
        dto.setId(r.getId());
        dto.setName(r.getName());
        dto.setCode(r.getCode());
        dto.setDescription(r.getDescription());
        dto.setCreatedAt(r.getCreatedAt());
        return dto;
    }
}
