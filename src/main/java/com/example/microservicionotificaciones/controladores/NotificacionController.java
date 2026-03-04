package com.example.microservicionotificaciones.controladores;

import com.example.microservicionotificaciones.modelos.Notificacion;
import com.example.microservicionotificaciones.servicios.NotificacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/notificaciones")
@Slf4j
public class NotificacionController {

    @Autowired
    private NotificacionService notificacionService;

    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<Notificacion>> obtenerPorUsuario(@PathVariable UUID usuarioId) {
        log.info("Obteniendo notificaciones para el usuario: {}", usuarioId);
        return ResponseEntity.ok(notificacionService.obtenerPorUsuario(usuarioId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Notificacion> obtenerPorId(@PathVariable Integer id) {
        Optional<Notificacion> opt = notificacionService.obtenerPorId(id);
        return opt.map(ResponseEntity::ok)
                .orElseGet(() -> {
                    log.warn("Notificación con ID {} no encontrada", id);
                    return ResponseEntity.notFound().build();
                });
    }

    @PostMapping
    public ResponseEntity<Notificacion> crear(@RequestBody Notificacion notificacion) {
        log.info("Creando nueva notificación de tipo: {}", notificacion.getTipo());
        return ResponseEntity.ok(notificacionService.crearNotificacion(notificacion));
    }

    @PutMapping("/{id}/leer")
    public ResponseEntity<Notificacion> marcarComoLeida(@PathVariable Integer id) {
        Notificacion n = notificacionService.marcarComoLeida(id);
        if (n != null) {
            return ResponseEntity.ok(n);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarNotificacion(@PathVariable Integer id) {
        log.info("Intentando eliminar notificación con ID: {}", id);
        try {
            notificacionService.eliminarNotificacion(id);
            log.info("Notificación {} eliminada correctamente", id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.error("Error al eliminar notificación: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}