package com.redlining.controller;

import com.redlining.dto.OrderCreateRequest;
import com.redlining.dto.OrderDto;
import com.redlining.entity.User;
import com.redlining.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<Page<OrderDto>> list(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(orderService.list(user.getId(), page, size));
    }

    @PostMapping
    public ResponseEntity<OrderDto> create(@AuthenticationPrincipal User user,
                                            @RequestBody OrderCreateRequest request) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        OrderDto dto = orderService.create(
                user.getId(),
                request.getPlanId(),
                request.getPaymentMethod(),
                Boolean.TRUE.equals(request.getRenewal()));
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/{id}")
    public OrderDto get(@AuthenticationPrincipal User user, @PathVariable Long id) {
        if (user == null) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return orderService.get(id, user.getId());
    }

    /** Simulate payment success (for testing). In production, payment gateway calls a webhook. */
    @PostMapping("/{id}/complete")
    public OrderDto complete(@AuthenticationPrincipal User user, @PathVariable Long id) {
        if (user == null) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return orderService.complete(id, user.getId());
    }
}
