package com.example.microservicionotificaciones.dto;

public class PreferenciasDTO {
    private boolean pushEnabled;
    private boolean emailEnabled;
    private boolean recordatoriosCitas;
    private boolean recordatoriosActividades;
    private boolean recordatoriosHabitos;
    private boolean nuevasActividades;
    private boolean mensajesIa;

    public PreferenciasDTO() {
    }

    public boolean isPushEnabled() {
        return pushEnabled;
    }

    public void setPushEnabled(boolean pushEnabled) {
        this.pushEnabled = pushEnabled;
    }

    public boolean isEmailEnabled() {
        return emailEnabled;
    }

    public void setEmailEnabled(boolean emailEnabled) {
        this.emailEnabled = emailEnabled;
    }

    public boolean isRecordatoriosCitas() {
        return recordatoriosCitas;
    }

    public void setRecordatoriosCitas(boolean recordatoriosCitas) {
        this.recordatoriosCitas = recordatoriosCitas;
    }

    public boolean isRecordatoriosActividades() {
        return recordatoriosActividades;
    }

    public void setRecordatoriosActividades(boolean recordatoriosActividades) {
        this.recordatoriosActividades = recordatoriosActividades;
    }

    public boolean isRecordatoriosHabitos() {
        return recordatoriosHabitos;
    }

    public void setRecordatoriosHabitos(boolean recordatoriosHabitos) {
        this.recordatoriosHabitos = recordatoriosHabitos;
    }

    public boolean isNuevasActividades() {
        return nuevasActividades;
    }

    public void setNuevasActividades(boolean nuevasActividades) {
        this.nuevasActividades = nuevasActividades;
    }

    public boolean isMensajesIa() {
        return mensajesIa;
    }

    public void setMensajesIa(boolean mensajesIa) {
        this.mensajesIa = mensajesIa;
    }
}
