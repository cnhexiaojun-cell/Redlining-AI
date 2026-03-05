package com.redlining.dto.admin;

import jakarta.validation.constraints.Size;

import java.util.List;

public class UserUpdateRequest {

    @Size(max = 255)
    private String email;

    @Size(max = 64)
    private String realName;

    private Boolean enabled;
    private Long organizationId;
    private List<Long> roleIds;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public Long getOrganizationId() { return organizationId; }
    public void setOrganizationId(Long organizationId) { this.organizationId = organizationId; }
    public List<Long> getRoleIds() { return roleIds; }
    public void setRoleIds(List<Long> roleIds) { this.roleIds = roleIds; }
}
