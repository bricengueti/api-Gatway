package com.example.api_gatway.utils;

import java.util.List;

public class ErrorResponse {
    private String status;
    private List<String> errors;
    private int statusCode;

    public ErrorResponse(String status, List<String> errors, int statusCode) {
        this.status = status;
        this.errors = errors;
        this.statusCode = statusCode;
    }

    // Getters et setters
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }
}