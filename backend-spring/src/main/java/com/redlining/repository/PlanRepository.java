package com.redlining.repository;

import com.redlining.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlanRepository extends JpaRepository<Plan, Long> {

    Optional<Plan> findByCode(String code);

    Optional<Plan> findByDefaultPlanTrue();

    List<Plan> findAllByOrderBySortOrderAsc();
}
