package com.example.microservicionotificaciones.modelos;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "Notificaciones")
@Data
public class Notificacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "usuario_id", nullable = false)
    private Integer usuarioId;

    @Column(length = 100)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String mensaje;

    @Column(length = 50)
    private String tipo; // PUSH, EMAIL, SISTEMA

    @Column(length = 20)
    private String estado; // PENDIENTE, ENVIADO, LEIDO, FALLIDO

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_envio")
    private LocalDateTime fechaEnvio;

    private Boolean leida;

    @PrePersist
    public void prePersist() {
        this.fechaCreacion = LocalDateTime.now();
        if (this.estado == null)
            this.estado = "PENDIENTE";
        if (this.leida == null)
            this.leida = false;
    }
}