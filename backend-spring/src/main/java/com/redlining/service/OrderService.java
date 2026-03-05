package com.redlining.service;

import com.redlining.dto.OrderDto;
import com.redlining.entity.Order;
import com.redlining.entity.Plan;
import com.redlining.entity.User;
import com.redlining.repository.OrderRepository;
import com.redlining.repository.PlanRepository;
import com.redlining.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final PlanRepository planRepository;
    private final UserRepository userRepository;
    private final WeChatPayService weChatPayService;

    public OrderService(OrderRepository orderRepository, PlanRepository planRepository, UserRepository userRepository,
                        WeChatPayService weChatPayService) {
        this.orderRepository = orderRepository;
        this.planRepository = planRepository;
        this.userRepository = userRepository;
        this.weChatPayService = weChatPayService;
    }

    @Transactional
    public OrderDto create(Long userId, Long planId, String paymentMethod, boolean renewal) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Order order = new Order();
        order.setUserId(userId);
        order.setPlanId(planId);
        order.setAmountCents(plan.getPriceCents());
        order.setPaymentMethod(paymentMethod != null ? paymentMethod : "wechat");
        order.setStatus("pending");
        order.setRenewal(renewal);
        order = orderRepository.save(order);

        OrderDto dto = toDto(order, plan.getName());
        if ("wechat".equalsIgnoreCase(order.getPaymentMethod()) && weChatPayService.isConfigured()) {
            String codeUrl = weChatPayService.createNativeOrder(
                    String.valueOf(order.getId()),
                    order.getAmountCents(),
                    "套餐购买-" + plan.getName());
            if (codeUrl != null) {
                dto.setCodeUrl(codeUrl);
            }
        }
        return dto;
    }

    /** Mark order as paid and apply plan to user (quota stack or subscription period stack). */
    @Transactional
    public OrderDto complete(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        if (!order.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Order does not belong to user");
        }
        if (!"pending".equals(order.getStatus())) {
            Plan p = planRepository.findById(order.getPlanId()).orElse(null);
            return toDto(order, p != null ? p.getName() : null);
        }
        order.setStatus("paid");
        order.setPaidAt(Instant.now());
        orderRepository.save(order);
        applyOrderToUser(order);
        Plan p = planRepository.findById(order.getPlanId()).orElse(null);
        return toDto(order, p != null ? p.getName() : null);
    }

    /** Called when WeChat Pay notification confirms payment. Idempotent: if order already paid, no-op. */
    @Transactional
    public void completeByPaymentNotification(Long orderId, String wechatTransactionId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            return;
        }
        if (!"pending".equals(order.getStatus())) {
            return;
        }
        order.setExternalOrderId(wechatTransactionId);
        order.setStatus("paid");
        order.setPaidAt(Instant.now());
        orderRepository.save(order);
        applyOrderToUser(order);
    }

    private void applyOrderToUser(Order order) {
        Plan plan = planRepository.findById(order.getPlanId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan not found"));
        User user = userRepository.findById(order.getUserId()).orElseThrow();
        if ("quota".equals(plan.getType())) {
            user.setPlanId(plan.getId());
            user.setQuotaRemaining(user.getQuotaRemaining() + plan.getQuota());
            user.setPeriodEndsAt(null);
            userRepository.save(user);
        } else {
            if (order.isRenewal() && user.getPeriodEndsAt() != null) {
                Instant next = "year".equals(plan.getPeriod())
                        ? user.getPeriodEndsAt().plus(365, ChronoUnit.DAYS)
                        : user.getPeriodEndsAt().plus(30, ChronoUnit.DAYS);
                user.setPeriodEndsAt(next);
            } else {
                user.setPlanId(plan.getId());
                Instant end = "year".equals(plan.getPeriod())
                        ? Instant.now().plus(365, ChronoUnit.DAYS)
                        : Instant.now().plus(30, ChronoUnit.DAYS);
                user.setPeriodEndsAt(end);
                user.setQuotaRemaining(plan.getQuota() <= 0 ? -1 : plan.getQuota());
            }
            userRepository.save(user);
        }
    }

    public OrderDto get(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        if (!order.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Order does not belong to user");
        }
        return toDto(order, null);
    }

    public Page<OrderDto> list(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(50, Math.max(1, size)));
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(o -> {
                    Plan plan = planRepository.findById(o.getPlanId()).orElse(null);
                    String planName = plan != null ? plan.getName() : null;
                    return toDto(o, planName);
                });
    }

    private static OrderDto toDto(Order o, String planName) {
        OrderDto dto = new OrderDto();
        dto.setId(o.getId());
        dto.setPlanId(o.getPlanId());
        dto.setAmountCents(o.getAmountCents());
        dto.setPaymentMethod(o.getPaymentMethod());
        dto.setStatus(o.getStatus());
        dto.setPaidAt(o.getPaidAt());
        dto.setCreatedAt(o.getCreatedAt());
        dto.setRenewal(o.isRenewal());
        if (planName != null) {
            dto.setPlanName(planName);
        }
        return dto;
    }
}
