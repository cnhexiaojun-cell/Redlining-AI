package com.redlining.service.admin;

import com.redlining.dto.admin.PlanCreateRequest;
import com.redlining.dto.admin.PlanDto;
import com.redlining.entity.Plan;
import com.redlining.entity.User;
import com.redlining.repository.PlanRepository;
import com.redlining.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminPlanService {

    private final PlanRepository planRepository;
    private final UserRepository userRepository;

    public AdminPlanService(PlanRepository planRepository, UserRepository userRepository) {
        this.planRepository = planRepository;
        this.userRepository = userRepository;
    }

    public List<PlanDto> list() {
        return planRepository.findAllByOrderBySortOrderAsc().stream().map(this::toDto).collect(Collectors.toList());
    }

    public PlanDto get(Long id) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan not found"));
        return toDto(plan);
    }

    @Transactional
    public PlanDto create(PlanCreateRequest req) {
        if (planRepository.findByCode(req.getCode()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Plan code already exists");
        }
        if (Boolean.TRUE.equals(req.getDefaultPlan())) {
            planRepository.findByDefaultPlanTrue().ifPresent(p -> {
                p.setDefaultPlan(false);
                planRepository.save(p);
            });
        }
        Plan plan = new Plan();
        plan.setCode(req.getCode().trim());
        plan.setName(req.getName().trim());
        plan.setType(req.getType().trim());
        plan.setQuota(req.getQuota() != null ? req.getQuota() : 0);
        plan.setPeriod(req.getPeriod() != null && !req.getPeriod().isBlank() ? req.getPeriod().trim() : null);
        plan.setPriceCents(req.getPriceCents() != null ? req.getPriceCents() : 0);
        plan.setDefaultPlan(Boolean.TRUE.equals(req.getDefaultPlan()));
        plan.setSortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0);
        plan.setDescription(req.getDescription() != null && !req.getDescription().isBlank() ? req.getDescription().trim() : null);
        plan.setScope(req.getScope() != null && !req.getScope().isBlank() ? req.getScope().trim() : null);
        plan = planRepository.save(plan);
        return toDto(plan);
    }

    @Transactional
    public PlanDto update(Long id, PlanCreateRequest req) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan not found"));
        if (planRepository.findByCode(req.getCode().trim()).filter(p -> !p.getId().equals(id)).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Plan code already exists");
        }
        if (Boolean.TRUE.equals(req.getDefaultPlan()) && !plan.isDefaultPlan()) {
            planRepository.findByDefaultPlanTrue().ifPresent(p -> {
                p.setDefaultPlan(false);
                planRepository.save(p);
            });
        }
        plan.setCode(req.getCode().trim());
        plan.setName(req.getName().trim());
        plan.setType(req.getType().trim());
        plan.setQuota(req.getQuota() != null ? req.getQuota() : 0);
        plan.setPeriod(req.getPeriod() != null && !req.getPeriod().isBlank() ? req.getPeriod().trim() : null);
        plan.setPriceCents(req.getPriceCents() != null ? req.getPriceCents() : 0);
        plan.setDefaultPlan(Boolean.TRUE.equals(req.getDefaultPlan()));
        plan.setSortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0);
        plan.setDescription(req.getDescription() != null && !req.getDescription().isBlank() ? req.getDescription().trim() : null);
        plan.setScope(req.getScope() != null && !req.getScope().isBlank() ? req.getScope().trim() : null);
        plan = planRepository.save(plan);
        return toDto(plan);
    }

    @Transactional
    public void delete(Long id) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan not found"));
        if (plan.isDefaultPlan()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot delete default plan");
        }
        long usersWithPlan = userRepository.findAll().stream().filter(u -> id.equals(u.getPlanId())).count();
        if (usersWithPlan > 0) {
            var defaultPlan = planRepository.findByDefaultPlanTrue()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "No default plan to migrate users to"));
            for (User u : userRepository.findAll()) {
                if (id.equals(u.getPlanId())) {
                    u.setPlanId(defaultPlan.getId());
                    u.setQuotaRemaining(defaultPlan.getQuota() <= 0 ? -1 : defaultPlan.getQuota());
                    u.setPeriodEndsAt("subscription".equals(defaultPlan.getType()) && defaultPlan.getPeriod() != null
                            ? java.time.Instant.now().plus("year".equals(defaultPlan.getPeriod()) ? 365 : 30, java.time.temporal.ChronoUnit.DAYS)
                            : null);
                    userRepository.save(u);
                }
            }
        }
        planRepository.delete(plan);
    }

    private PlanDto toDto(Plan p) {
        PlanDto dto = new PlanDto();
        dto.setId(p.getId());
        dto.setCode(p.getCode());
        dto.setName(p.getName());
        dto.setType(p.getType());
        dto.setQuota(p.getQuota());
        dto.setPeriod(p.getPeriod());
        dto.setPriceCents(p.getPriceCents());
        dto.setDefaultPlan(p.isDefaultPlan());
        dto.setSortOrder(p.getSortOrder());
        dto.setDescription(p.getDescription());
        dto.setScope(p.getScope());
        return dto;
    }
}
