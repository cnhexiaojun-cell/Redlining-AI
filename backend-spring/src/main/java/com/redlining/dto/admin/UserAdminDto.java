package com.redlining.dto.admin;

import java.time.Instant;
import java.util.List;

public class UserAdminDto {

    private Long id;
    private String username;
    private String email;
    private String realName;
    private Boolean enabled;
    private Boolean superAdmin;
    private Long organizationId;
    private String organizationName;
    private Instant createdAt;
    private List<Long> roleIds;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public Boolean getSuperAdmin() { return superAdmin; }
    public void setSuperAdmin(Boolean superAdmin) { this.superAdmin = superAdmin; }
    public Long getOrganizationId() { return organizationId; }
    public void setOrganizationId(Long organizationId) { this.organizationId = organizationId; }
    public String getOrganizationName() { return organizationName; }
    public void setOrganizationName(String organizationName) { this.organizationName = organizationName; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public List<Long> getRoleIds() { return roleIds; }
    public void setRoleIds(List<Long> roleIds) { this.roleIds = roleIds; }
}
