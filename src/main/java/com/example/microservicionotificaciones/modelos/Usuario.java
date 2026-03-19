package com.example.microservicionotificaciones.modelos;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "usuarios")
public class Usuario {
    @Id
    @Column(columnDefinition = "NVARCHAR(255)")
    private String id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    private String rol = "USUARIO";



    // --- CONSTRUCTORES ---
    public Usuario() {
    }

    // --- GETTERS Y SETTERS MANUALES (Adiós Lombok) ---
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    @Column(name = "push_enabled")
    private Boolean pushEnabled = true;

    @Column(name = "email_enabled")
    private Boolean emailEnabled = true;

    @Column(name = "recordatorios_citas")
    private Boolean recordatoriosCitas = true;

    @Column(name = "recordatorios_actividades")
    private Boolean recordatoriosActividades = true;

    @Column(name = "recordatorios_habitos")
    private Boolean recordatoriosHabitos = true;

    @Column(name = "nuevas_actividades")
    private Boolean nuevasActividades = false;

    @Column(name = "mensajes_ia")
    private Boolean mensajesIa = true;

    public boolean isPushEnabled() {
        return pushEnabled != null ? pushEnabled : true;
    }

    public void setPushEnabled(Boolean pushEnabled) {
        this.pushEnabled = pushEnabled;
    }

    public boolean isEmailEnabled() {
        return emailEnabled != null ? emailEnabled : true;
    }

    public void setEmailEnabled(Boolean emailEnabled) {
        this.emailEnabled = emailEnabled;
    }

    public boolean isRecordatoriosCitas() {
        return recordatoriosCitas != null ? recordatoriosCitas : true;
    }

    public void setRecordatoriosCitas(Boolean recordatoriosCitas) {
        this.recordatoriosCitas = recordatoriosCitas;
    }

    public boolean isRecordatoriosActividades() {
        return recordatoriosActividades != null ? recordatoriosActividades : true;
    }

    public void setRecordatoriosActividades(Boolean recordatoriosActividades) {
        this.recordatoriosActividades = recordatoriosActividades;
    }

    public boolean isRecordatoriosHabitos() {
        return recordatoriosHabitos != null ? recordatoriosHabitos : true;
    }

    public void setRecordatoriosHabitos(Boolean recordatoriosHabitos) {
        this.recordatoriosHabitos = recordatoriosHabitos;
    }

    public boolean isNuevasActividades() {
        return nuevasActividades != null ? nuevasActividades : false;
    }

    public void setNuevasActividades(Boolean nuevasActividades) {
        this.nuevasActividades = nuevasActividades;
    }

    public boolean isMensajesIa() {
        return mensajesIa != null ? mensajesIa : true;
    }

    public void setMensajesIa(Boolean mensajesIa) {
        this.mensajesIa = mensajesIa;
    }
}