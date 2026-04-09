package com.example.microservicionotificaciones.modelos;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "citas_notificadas")
public class CitaNotificada {

    @Id
    private Integer citaId;

    private boolean creacionNotificada;
    private boolean recordatorioLanzado;

    // Constructores, Getters y Setters
    public CitaNotificada() {}

    public CitaNotificada(Integer citaId) {
        this.citaId = citaId;
    }

    public Integer getCitaId() { return citaId; }
    public void setCitaId(Integer citaId) { this.citaId = citaId; }

    public boolean isCreacionNotificada() { return creacionNotificada; }
    public void setCreacionNotificada(boolean creacionNotificada) { this.creacionNotificada = creacionNotificada; }

    public boolean isRecordatorioLanzado() { return recordatorioLanzado; }
    public void setRecordatorioLanzado(boolean recordatorioLanzado) { this.recordatorioLanzado = recordatorioLanzado; }
}
