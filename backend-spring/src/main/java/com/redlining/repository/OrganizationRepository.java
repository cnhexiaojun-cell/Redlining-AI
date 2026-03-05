package com.redlining.repository;

import com.redlining.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    List<Organization> findByParentIdOrderBySortOrderAsc(Long parentId);

    List<Organization> findByParentIdIsNullOrderBySortOrderAsc();

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);
}
