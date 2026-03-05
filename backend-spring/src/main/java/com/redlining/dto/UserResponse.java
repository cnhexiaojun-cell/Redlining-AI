package com.redlining.dto;

public class UserResponse {

    private Long id;
    private String username;
    private String email;
    private String avatarUrl;
    private String realName;
    private String occupation;
    private String planCode;
    private String planName;
    private String planType;
    private Integer quotaRemaining;
    private Integer quotaTotal;
    private String periodEndsAt;

    public UserResponse() {
    }

    public UserResponse(Long id, String username, String email, String avatarUrl, String realName, String occupation) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.avatarUrl = avatarUrl;
        this.realName = realName;
        this.occupation = occupation;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getOccupation() {
        return occupation;
    }

    public void setOccupation(String occupation) {
        this.occupation = occupation;
    }

    public String getPlanCode() { return planCode; }
    public void setPlanCode(String planCode) { this.planCode = planCode; }
    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }
    public String getPlanType() { return planType; }
    public void setPlanType(String planType) { this.planType = planType; }
    public Integer getQuotaRemaining() { return quotaRemaining; }
    public void setQuotaRemaining(Integer quotaRemaining) { this.quotaRemaining = quotaRemaining; }
    public Integer getQuotaTotal() { return quotaTotal; }
    public void setQuotaTotal(Integer quotaTotal) { this.quotaTotal = quotaTotal; }
    public String getPeriodEndsAt() { return periodEndsAt; }
    public void setPeriodEndsAt(String periodEndsAt) { this.periodEndsAt = periodEndsAt; }
}
