package com.redlining.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "orders", indexes = {
    @Index(columnList = "user_id"),
    @Index(columnList = "plan_id"),
    @Index(columnList = "status"),
    @Index(columnList = "created_at")
})
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Column(name = "amount_cents", nullable = false)
    private int amountCents;

    /** wechat / alipay */
    @Column(name = "payment_method", nullable = true, length = 32)
    private String paymentMethod;

    /** pending / paid / cancelled */
    @Column(nullable = false, length = 32)
    private String status = "pending";

    @Column(name = "external_order_id", nullable = true, length = 128)
    private String externalOrderId;

    @Column(name = "paid_at", nullable = true)
    private Instant paidAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    /** true = 续费（订阅有效期叠加） */
    @Column(name = "is_renewal", nullable = false)
    private boolean renewal = false;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }
    public int getAmountCents() { return amountCents; }
    public void setAmountCents(int amountCents) { this.amountCents = amountCents; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getExternalOrderId() { return externalOrderId; }
    public void setExternalOrderId(String externalOrderId) { this.externalOrderId = externalOrderId; }
    public Instant getPaidAt() { return paidAt; }
    public void setPaidAt(Instant paidAt) { this.paidAt = paidAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public boolean isRenewal() { return renewal; }
    public void setRenewal(boolean renewal) { this.renewal = renewal; }
}
