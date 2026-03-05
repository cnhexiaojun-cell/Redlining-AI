package com.redlining.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "plans", indexes = {
    @Index(columnList = "code", unique = true),
    @Index(columnList = "is_default"),
    @Index(columnList = "type")
})
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String code;

    @Column(nullable = false, length = 64)
    private String name;

    /** quota = 按次套餐, subscription = 订阅套餐 */
    @Column(nullable = false, length = 16)
    private String type = "quota";

    /** 按次: 总次数; 订阅: 每周期次数, -1 = 不限次 */
    @Column(nullable = false)
    private int quota = 0;

    /** 仅订阅: month / year */
    @Column(nullable = true, length = 16)
    private String period;

    @Column(name = "price_cents", nullable = false)
    private int priceCents = 0;

    @Column(name = "is_default", nullable = false)
    private boolean defaultPlan = false;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Column(columnDefinition = "TEXT", nullable = true)
    private String description;

    @Column(nullable = true, length = 64)
    private String scope;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public int getQuota() { return quota; }
    public void setQuota(int quota) { this.quota = quota; }
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    public int getPriceCents() { return priceCents; }
    public void setPriceCents(int priceCents) { this.priceCents = priceCents; }
    public boolean isDefaultPlan() { return defaultPlan; }
    public void setDefaultPlan(boolean defaultPlan) { this.defaultPlan = defaultPlan; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
