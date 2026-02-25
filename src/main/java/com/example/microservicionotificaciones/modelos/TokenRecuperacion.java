package com.example.microservicionotificaciones.modelos;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "TokenRecuperacion")
@Data
public class TokenRecuperacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 255)
    private String token;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;

    @Column
    private Boolean usado = false;

    @PrePersist
    public void prePersist() {
        if (this.usado == null)
            this.usado = false;
    }
}
