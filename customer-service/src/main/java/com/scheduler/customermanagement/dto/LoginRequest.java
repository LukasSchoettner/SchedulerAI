package com.scheduler.customermanagement.dto;

public class LoginRequest {
    private String customername;
    private String password;

    public LoginRequest() {
    }

    public LoginRequest(String customername, String password) {
        this.customername = customername;
        this.password = password;
    }

    public String getCustomername() {
        return customername;
    }
    public void setCustomername(String customername) {
        this.customername = customername;
    }

    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
}
