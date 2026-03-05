package com.redlining.dto.admin;

public class PlanDto {

    private Long id;
    private String code;
    private String name;
    private String type;
    private Integer quota;
    private String period;
    private Integer priceCents;
    private Boolean defaultPlan;
    private Integer sortOrder;
    private String description;
    private String scope;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
