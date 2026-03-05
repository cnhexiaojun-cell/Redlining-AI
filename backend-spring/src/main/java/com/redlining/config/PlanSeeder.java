package com.redlining.config;

import com.redlining.entity.Plan;
import com.redlining.entity.User;
import com.redlining.repository.PlanRepository;
import com.redlining.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds default free plan if plans table is empty; backfills existing users with default plan.
 */
@Component
@Order(101)
public class PlanSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PlanSeeder.class);

    private final PlanRepository planRepository;
    private final UserRepository userRepository;

    public PlanSeeder(PlanRepository planRepository, UserRepository userRepository) {
        this.planRepository = planRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (planRepository.count() > 0) {
            backfillUsersWithoutPlan();
            return;
        }
        log.info("Seeding default plan");
        Plan free = new Plan();
        free.setCode("free");
        free.setName("免费版");
        free.setType("quota");
        free.setQuota(5);
        free.setPeriod(null);
        free.setPriceCents(0);
        free.setDefaultPlan(true);
        free.setSortOrder(10);
        free.setDescription("5次合同分析");
        free.setScope("个人用户");
        free = planRepository.save(free);

        Plan subscription = new Plan();
        subscription.setCode("pro-monthly");
        subscription.setName("专业版");
        subscription.setType("subscription");
        subscription.setQuota(50);
        subscription.setPeriod("month");
        subscription.setPriceCents(9900);
        subscription.setDefaultPlan(false);
        subscription.setSortOrder(20);
        subscription.setDescription("每月50次合同分析，到期可续费");
        subscription.setScope("个人与团队");
        planRepository.save(subscription);

        for (User u : userRepository.findAll()) {
            if (u.getPlanId() == null) {
                u.setPlanId(free.getId());
                u.setQuotaRemaining(free.getQuota());
                u.setPeriodEndsAt(null);
                userRepository.save(u);
            }
        }
        log.info("Seeded default plan and backfilled users");
    }

    private void backfillUsersWithoutPlan() {
        var defaultPlan = planRepository.findByDefaultPlanTrue();
        if (defaultPlan.isEmpty()) return;
        Plan plan = defaultPlan.get();
        int count = 0;
        for (User u : userRepository.findAll()) {
            if (u.getPlanId() == null) {
                u.setPlanId(plan.getId());
                u.setQuotaRemaining(plan.getQuota());
                u.setPeriodEndsAt(null);
                userRepository.save(u);
                count++;
            }
        }
        if (count > 0) log.info("Backfilled {} users with default plan", count);
    }
}
