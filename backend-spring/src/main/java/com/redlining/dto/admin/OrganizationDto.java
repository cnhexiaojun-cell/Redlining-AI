package com.redlining.dto.admin;

import java.time.Instant;
import java.util.List;

public class OrganizationDto {

    private Long id;
    private String name;
    private String code;
    private Long parentId;
    private int sortOrder;
    private Instant createdAt;
    private List<OrganizationDto> children;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public List<OrganizationDto> getChildren() { return children; }
    public void setChildren(List<OrganizationDto> children) { this.children = children; }
}
