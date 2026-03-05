package com.redlining.dto;

public class LoginResponse {

    private String accessToken;
    private String tokenType = "bearer";
    private UserInfo user;

    public LoginResponse() {
    }

    public LoginResponse(String accessToken, UserInfo user) {
        this.accessToken = accessToken;
        this.user = user;
    }

    public String getAccess_token() {
        return accessToken;
    }

    public void setAccess_token(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getToken_type() {
        return tokenType;
    }

    public void setToken_type(String tokenType) {
        this.tokenType = tokenType;
    }

    public UserInfo getUser() {
        return user;
    }

    public void setUser(UserInfo user) {
        this.user = user;
    }

    public static class UserInfo {
        private Long id;
        private String username;
        private String email;

        public UserInfo() {
        }

        public UserInfo(Long id, String username, String email) {
            this.id = id;
            this.username = username;
            this.email = email;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }
}
