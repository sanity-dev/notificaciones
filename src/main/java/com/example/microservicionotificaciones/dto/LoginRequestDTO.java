package com.example.microservicionotificaciones.dto;

// Quitamos Lombok porque lo haremos manual
// import lombok.Data;

public class LoginRequestDTO {
    private String email;
    private String password;

    // --- Constructor Vacío ---
    public LoginRequestDTO() {
    }

    // --- Constructor con Argumentos ---
    public LoginRequestDTO(String email, String password) {
        this.email = email;
        this.password = password;
    }

    // --- GETTERS Y SETTERS MANUALES ---
    // Estos son los métodos que Java no encontraba

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}