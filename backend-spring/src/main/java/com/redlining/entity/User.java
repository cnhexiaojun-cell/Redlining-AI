package com.redlining.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "users", indexes = {
    @Index(columnList = "username", unique = true),
    @Index(columnList = "email", unique = true),
    @Index(columnList = "organization_id")
})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = true, unique = true, length = 64)
    private String username;

    @Column(nullable = true, unique = true, length = 255)
    private String email;

    @Column(name = "avatar_url", nullable = true, length = 255)
    private String avatarUrl;

    @Column(name = "real_name", nullable = true, length = 64)
    private String realName;

    @Column(nullable = true, length = 128)
    private String occupation;

    @Column(name = "hashed_password", nullable = false, length = 255)
    private String hashedPassword;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "is_super_admin", nullable = false)
    private boolean superAdmin = false;

    @Column(name = "organization_id", nullable = true)
    private Long organizationId;

    @Column(name = "plan_id", nullable = true)
    private Long planId;

    @Column(name = "quota_remaining", nullable = false)
    private int quotaRemaining = 0;

    @Column(name = "period_ends_at", nullable = true)
    private Instant periodEndsAt;

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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public String getHashedPassword() {
        return hashedPassword;
    }

    public void setHashedPassword(String hashedPassword) {
        this.hashedPassword = hashedPassword;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isSuperAdmin() {
        return superAdmin;
    }

    public void setSuperAdmin(boolean superAdmin) {
        this.superAdmin = superAdmin;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }
    public int getQuotaRemaining() { return quotaRemaining; }
    public void setQuotaRemaining(int quotaRemaining) { this.quotaRemaining = quotaRemaining; }
    public Instant getPeriodEndsAt() { return periodEndsAt; }
    public void setPeriodEndsAt(Instant periodEndsAt) { this.periodEndsAt = periodEndsAt; }
}
