package com.redlining.dto;

import java.time.Instant;

public class OrderDto {
    private Long id;
    private Long planId;
    private int amountCents;
    private String paymentMethod;
    private String status;
    private Instant paidAt;
    private Instant createdAt;
    private boolean renewal;
    /** WeChat Pay Native code_url for QR code; only set when paymentMethod is wechat and backend created native order. */
    private String codeUrl;
    /** Plan name for display in order list. */
    private String planName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }
    public int getAmountCents() { return amountCents; }
    public void setAmountCents(int amountCents) { this.amountCents = amountCents; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getPaidAt() { return paidAt; }
    public void setPaidAt(Instant paidAt) { this.paidAt = paidAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public boolean isRenewal() { return renewal; }
    public void setRenewal(boolean renewal) { this.renewal = renewal; }
    public String getCodeUrl() { return codeUrl; }
    public void setCodeUrl(String codeUrl) { this.codeUrl = codeUrl; }
    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }
}
