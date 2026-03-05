package com.redlining.dto;

import java.util.Set;

public class PermissionsResponse {

    private boolean superAdmin;
    private Set<String> menuCodes;
    private Set<String> buttonCodes;
    private Set<String> dataScope;

    public PermissionsResponse() {
    }

    public PermissionsResponse(boolean superAdmin, Set<String> menuCodes, Set<String> buttonCodes, String dataScopeValue) {
        this.superAdmin = superAdmin;
        this.menuCodes = menuCodes;
        this.buttonCodes = buttonCodes;
        this.dataScope = dataScopeValue != null ? Set.of(dataScopeValue) : Set.of();
    }

    public boolean isSuperAdmin() {
        return superAdmin;
    }

    public void setSuperAdmin(boolean superAdmin) {
        this.superAdmin = superAdmin;
    }

    public Set<String> getMenuCodes() {
        return menuCodes;
    }

    public void setMenuCodes(Set<String> menuCodes) {
        this.menuCodes = menuCodes;
    }

    public Set<String> getButtonCodes() {
        return buttonCodes;
    }

    public void setButtonCodes(Set<String> buttonCodes) {
        this.buttonCodes = buttonCodes;
    }

    public Set<String> getDataScope() {
        return dataScope;
    }

    public void setDataScope(Set<String> dataScope) {
        this.dataScope = dataScope;
    }
}
