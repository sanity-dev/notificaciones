package com.example.microservicionotificaciones.controladores;

import com.example.microservicionotificaciones.modelos.Notificacion;
import com.example.microservicionotificaciones.modelos.Usuario;
import com.example.microservicionotificaciones.servicios.NotificacionService;
import com.example.microservicionotificaciones.seguridad.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@Slf4j
public class NotificacionController {

    @Autowired
    private NotificacionService notificacionService;

    @Autowired
    private com.example.microservicionotificaciones.repositorios.UsuarioRepository usuarioRepository;

    @Autowired
    private JwtService jwtService;

    @GetMapping("/me")
    public ResponseEntity<List<Notificacion>> obtenerMisNotificaciones(
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 1. Intentar obtener email del header del Gateway
        String email = userEmail;

        // 2. Si no hay header del gateway, intentar extraer del JWT directamente
        if (email == null && authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                email = jwtService.extractUsername(token);
            } catch (Exception e) {
                log.warn("No se pudo extraer email del JWT: {}", e.getMessage());
            }
        }

        if (email == null || email.isBlank()) {
            log.warn("Intento de acceso a notificaciones sin identificación de usuario");
            return ResponseEntity.status(401).build();
        }

        log.info("Obteniendo notificaciones para el usuario: {}", email);

        Optional<Usuario> optUsuario = usuarioRepository.findByEmail(email);
        if (optUsuario.isPresent()) {
            Usuario usuario = optUsuario.get();
            log.info("Usuario encontrado en BD local. ID: {}", usuario.getId());
            return ResponseEntity.ok(notificacionService.obtenerPorUsuario(usuario.getId()));
        } else {
            log.warn("Usuario con email {} no existe en la base de datos local de NOTIFICACIONES.", email);
            // Devolvemos una lista vacía en vez de 404 para no romper el frontend
            // mientras se sincronizan los usuarios.
            return ResponseEntity.ok(java.util.Collections.emptyList());
        }
    }

    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<Notificacion>> obtenerPorUsuario(@PathVariable UUID usuarioId) {
        log.info("Obteniendo notificaciones para el usuario solicitado: {}", usuarioId);
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