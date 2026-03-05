package com.redlining.dto.admin;

import java.util.List;

public class PermissionTreeDto {

    private Long id;
    private String code;
    private String name;
    private String type;
    private Long parentId;
    private int sortOrder;
    private List<PermissionTreeDto> children;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public List<PermissionTreeDto> getChildren() { return children; }
    public void setChildren(List<PermissionTreeDto> children) { this.children = children; }
}
