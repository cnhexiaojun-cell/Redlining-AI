package com.redlining.dto.admin;

import jakarta.validation.constraints.Size;

public class ResetPasswordRequest {

    @Size(min = 6, max = 128)
    private String password;

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
