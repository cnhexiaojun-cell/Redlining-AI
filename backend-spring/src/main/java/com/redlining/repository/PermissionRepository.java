package com.redlining.repository;

import com.redlining.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PermissionRepository extends JpaRepository<Permission, Long> {

    Optional<Permission> findByCode(String code);

    List<Permission> findByParentIdOrderBySortOrderAsc(Long parentId);

    List<Permission> findByParentIdIsNullOrderBySortOrderAsc();

    List<Permission> findByTypeOrderBySortOrderAsc(String type);
}
