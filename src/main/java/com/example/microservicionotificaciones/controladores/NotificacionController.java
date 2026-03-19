package com.example.microservicionotificaciones.controladores;

import com.example.microservicionotificaciones.modelos.Notificacion;
import com.example.microservicionotificaciones.modelos.Usuario;
import com.example.microservicionotificaciones.servicios.NotificacionService;
import com.example.microservicionotificaciones.seguridad.JwtService;
import com.example.microservicionotificaciones.servicios.SseNotificationService;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/notifications")
public class NotificacionController {

    private static final Logger log = LoggerFactory.getLogger(NotificacionController.class);

    @Autowired
    private SseNotificationService sseNotificationService;

    @Autowired
    private NotificacionService notificacionService;

    @Autowired
    private com.example.microservicionotificaciones.repositorios.UsuarioRepository usuarioRepository;

    @Autowired
    private JwtService jwtService;

    @GetMapping("/me")
    public ResponseEntity<List<Notificacion>> obtenerMisNotificaciones(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Name", required = false) String userName,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        String email = userEmail;
        String idFromHeader = userId;

        // Si no hay headers del gateway, intentar extraer del JWT
        if ((email == null || idFromHeader == null) && authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                email = jwtService.extractUsername(token);
                // Aquí podrías extraer el ID si el JWT lo tiene, si no usamos el email
            } catch (Exception e) {
                log.warn("No se pudo extraer información del JWT: {}", e.getMessage());
            }
        }

        if (email == null || email.isBlank()) {
            log.warn("Intento de acceso a notificaciones sin identificación de usuario");
            return ResponseEntity.status(401).build();
        }

        log.info("Accediendo a notificaciones para: {}", email);

        final String finalEmail = email;
        final String finalId = idFromHeader;
        final String finalName = userName;

        // Auto-aprovisionamiento: Si no existe el usuario localmente, lo creamos
        Usuario usuario = usuarioRepository.findByEmail(finalEmail).orElseGet(() -> {
            log.info("Usuario {} no encontrado. Creando registro local (Auto-aprovisionamiento)...", finalEmail);
            Usuario nuevo = new Usuario();
            nuevo.setId(finalId != null ? finalId : finalEmail); // Usar ID del header o email como fallback
            nuevo.setEmail(finalEmail);
            nuevo.setNombre(finalName != null ? finalName : finalEmail.split("@")[0]);
            nuevo.setPassword("EXTERNAL_USER"); // No se requiere password real aquí
            return usuarioRepository.save(nuevo);
        });

        return ResponseEntity.ok(notificacionService.obtenerPorUsuario(usuario.getId()));
    }

    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<Notificacion>> obtenerPorUsuario(@PathVariable String usuarioId) {
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

    // --- SSE PUSH NOTIFICATIONS ENDPOINTS ---

    @GetMapping(value = "/stream/{usuarioId}", produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeToNotifications(@PathVariable String usuarioId) {
        log.info("Nuevo cliente SSE conectado: {}", usuarioId);
        return sseNotificationService.subscribe(usuarioId);
    }

    // --- TEST ENDPOINTS FOR PUSH TRIGGERS ---

    @PostMapping("/test-citas/{usuarioId}")
    public ResponseEntity<Notificacion> testCitaNotificacion(@PathVariable String usuarioId) {
        Notificacion notificacion = new Notificacion();
        notificacion.setUsuarioId(usuarioId);
        notificacion.setTitulo("Recordatorio de Cita Próxima");
        notificacion.setMensaje("Tienes una cita programada con tu especialista mañana a las 10:00 AM.");
        notificacion.setTipo("PUSH");
        return ResponseEntity.ok(notificacionService.crearNotificacion(notificacion));
    }

    @PostMapping("/test-habitos/{usuarioId}")
    public ResponseEntity<Notificacion> testHabitosNotificacion(@PathVariable String usuarioId) {
        Notificacion notificacion = new Notificacion();
        notificacion.setUsuarioId(usuarioId);
        notificacion.setTitulo("Recomendación de Hábitos");
        notificacion.setMensaje("Es un buen momento para tomar un vaso de agua y estirarte por 5 minutos.");
        notificacion.setTipo("PUSH");
        return ResponseEntity.ok(notificacionService.crearNotificacion(notificacion));
    }

    @PostMapping("/test-euphoria/{usuarioId}")
    public ResponseEntity<Notificacion> testEuphoriaNotificacion(@PathVariable String usuarioId) {
        Notificacion notificacion = new Notificacion();
        notificacion.setUsuarioId(usuarioId);
        notificacion.setTitulo("¡Euphoria te extraña!");
        notificacion.setMensaje("Hace tiempo que no hablamos. ¿Cómo te sientes hoy? Escríbeme cuando quieras.");
        notificacion.setTipo("PUSH");
        return ResponseEntity.ok(notificacionService.crearNotificacion(notificacion));
    }
}