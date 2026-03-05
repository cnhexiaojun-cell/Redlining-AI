package com.redlining.service;

import com.redlining.entity.Plan;
import com.redlining.entity.User;
import com.redlining.repository.PlanRepository;
import com.redlining.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Contract analysis usage: check and deduct quota (per-call or subscription).
 */
@Service
public class UsageService {

    private final UserRepository userRepository;
    private final PlanRepository planRepository;

    public UsageService(UserRepository userRepository, PlanRepository planRepository) {
        this.userRepository = userRepository;
        this.planRepository = planRepository;
    }

    /**
     * Consume one analysis for the user. Call before performing analysis.
     * Throws 403 with code "quota_exhausted" if no quota left.
     */
    @Transactional
    public void consumeOneAnalysis(User user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        if (user.isSuperAdmin()) {
            return; // no limit for super admin
        }
        User fresh = userRepository.findById(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        Plan plan = null;
        if (fresh.getPlanId() != null) {
            plan = planRepository.findById(fresh.getPlanId()).orElse(null);
        }
        if (plan == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "quota_exhausted");
        }

        // Subscription: advance period if expired
        if ("subscription".equals(plan.getType()) && fresh.getPeriodEndsAt() != null) {
            Instant now = Instant.now();
            if (!now.isBefore(fresh.getPeriodEndsAt())) {
                Instant nextEnd = nextPeriodEnd(fresh.getPeriodEndsAt(), plan.getPeriod());
                fresh.setPeriodEndsAt(nextEnd);
                fresh.setQuotaRemaining(plan.getQuota() <= 0 ? -1 : plan.getQuota());
                userRepository.save(fresh);
            }
        }

        // Unlimited (subscription with quota <= 0): no deduct
        if (plan.getQuota() <= 0) {
            return;
        }

        if (fresh.getQuotaRemaining() <= 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "quota_exhausted");
        }

        fresh.setQuotaRemaining(fresh.getQuotaRemaining() - 1);
        userRepository.save(fresh);
    }

    private static Instant nextPeriodEnd(Instant currentEnd, String period) {
        if (period == null) return currentEnd.plus(30, ChronoUnit.DAYS);
        return "year".equalsIgnoreCase(period)
                ? currentEnd.plus(365, ChronoUnit.DAYS)
                : currentEnd.plus(30, ChronoUnit.DAYS);
    }
}
