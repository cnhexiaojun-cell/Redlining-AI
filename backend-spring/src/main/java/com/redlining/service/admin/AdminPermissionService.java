package com.redlining.service.admin;

import com.redlining.dto.admin.PermissionTreeDto;
import com.redlining.entity.Permission;
import com.redlining.repository.PermissionRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminPermissionService {

    private final PermissionRepository permissionRepository;

    public AdminPermissionService(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    public List<PermissionTreeDto> getTree() {
        List<Permission> all = permissionRepository.findAll();
        Map<Long, List<Permission>> byParent = all.stream().collect(Collectors.groupingBy(p -> p.getParentId() == null ? -1L : p.getParentId()));
        return buildChildren(-1L, byParent);
    }

    private List<PermissionTreeDto> buildChildren(Long parentId, Map<Long, List<Permission>> byParent) {
        List<Permission> list = byParent.get(parentId);
        if (list == null) return List.of();
        list.sort((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()));
        List<PermissionTreeDto> result = new ArrayList<>();
        for (Permission p : list) {
            PermissionTreeDto dto = new PermissionTreeDto();
            dto.setId(p.getId());
            dto.setCode(p.getCode());
            dto.setName(p.getName());
            dto.setType(p.getType());
            dto.setParentId(p.getParentId());
            dto.setSortOrder(p.getSortOrder());
            dto.setChildren(buildChildren(p.getId(), byParent));
            result.add(dto);
        }
        return result;
    }
}
