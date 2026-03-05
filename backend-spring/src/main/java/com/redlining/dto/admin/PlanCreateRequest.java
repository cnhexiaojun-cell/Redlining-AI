package com.redlining.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class PlanCreateRequest {

    @NotBlank
    @Size(max = 32)
    private String code;

    @NotBlank
    @Size(max = 64)
    private String name;

    @NotBlank
    @Size(max = 16)
    private String type;

    @NotNull
    private Integer quota;

    @Size(max = 16)
    private String period;

    @NotNull
    private Integer priceCents;

    private Boolean defaultPlan = false;

    @NotNull
    private Integer sortOrder = 0;

    private String description;

    @Size(max = 64)
    private String scope;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Integer getQuota() { return quota; }
    public void setQuota(Integer quota) { this.quota = quota; }
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    public Integer getPriceCents() { return priceCents; }
    public void setPriceCents(Integer priceCents) { this.priceCents = priceCents; }
    public Boolean getDefaultPlan() { return defaultPlan; }
    public void setDefaultPlan(Boolean defaultPlan) { this.defaultPlan = defaultPlan; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
}
