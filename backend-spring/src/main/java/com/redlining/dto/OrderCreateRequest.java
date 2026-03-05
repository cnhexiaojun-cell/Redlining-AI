package com.redlining.dto;

public class OrderCreateRequest {

    private Long planId;
    private String paymentMethod; // wechat | alipay
    private Boolean renewal = false;

    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public Boolean getRenewal() { return renewal; }
    public void setRenewal(Boolean renewal) { this.renewal = renewal; }
}
