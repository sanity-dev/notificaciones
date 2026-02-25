package com.example.microservicionotificaciones.dto;

public class LoginResponseDTO {
    private String token;

    // Constructor vacío
    public LoginResponseDTO() {
    }

    // Constructor con token
    public LoginResponseDTO(String token) {
        this.token = token;
    }

    // Getter y Setter
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}