package com.example.microservicionotificaciones.repositorios;

import com.example.microservicionotificaciones.modelos.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Integer> {
    List<Notificacion> findByUsuarioId(UUID usuarioId);

    List<Notificacion> findByUsuarioIdAndLeidaFalse(UUID usuarioId);
}