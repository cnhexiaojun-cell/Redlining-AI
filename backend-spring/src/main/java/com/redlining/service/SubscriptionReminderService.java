package com.redlining.service;

import com.redlining.entity.Plan;
import com.redlining.entity.User;
import com.redlining.repository.PlanRepository;
import com.redlining.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Sends renewal reminders to subscription users whose period ends within 7 days.
 * Placeholder: logs and could be extended to send email/SMS.
 */
@Service
public class SubscriptionReminderService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionReminderService.class);
    private static final int REMINDER_DAYS = 7;

    private final UserRepository userRepository;
    private final PlanRepository planRepository;

    public SubscriptionReminderService(UserRepository userRepository, PlanRepository planRepository) {
        this.userRepository = userRepository;
        this.planRepository = planRepository;
    }

    @Scheduled(cron = "${app.subscription-reminder.cron:0 0 9 * * ?}") // default: daily at 9:00
    public void sendRenewalReminders() {
        Instant now = Instant.now();
        Instant limit = now.plusSeconds(REMINDER_DAYS * 24L * 3600);
        List<User> users = userRepository.findAll();
        for (User user : users) {
            if (user.getPeriodEndsAt() == null || user.getPlanId() == null) continue;
            if (user.getPeriodEndsAt().isBefore(now)) continue;
            if (user.getPeriodEndsAt().isAfter(limit)) continue;
            planRepository.findById(user.getPlanId()).ifPresent(plan -> {
                if (!"subscription".equals(plan.getType())) return;
                log.info("Subscription renewal reminder: userId={} username={} email={} periodEndsAt={}",
                        user.getId(), user.getUsername(), user.getEmail(), user.getPeriodEndsAt());
                // TODO: send email/SMS with renewal link (e.g. /plans)
            });
        }
    }
}
