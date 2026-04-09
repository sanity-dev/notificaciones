package com.example.microservicionotificaciones.servicios;

import com.example.microservicionotificaciones.modelos.Notificacion;
import com.example.microservicionotificaciones.repositorios.NotificacionRepository;
import com.example.microservicionotificaciones.repositorios.UsuarioRepository;
import com.example.microservicionotificaciones.modelos.Usuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Optional;

@Service
public class NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private SseNotificationService sseNotificationService;

    public List<Notificacion> obtenerPorUsuario(String usuarioId) {
        return notificacionRepository.findByUsuarioId(usuarioId);
    }

    public Optional<Notificacion> obtenerPorId(Integer id) {
        return notificacionRepository.findById(id);
    }

    public List<Notificacion> obtenerNoLeidas(String usuarioId) {
        return notificacionRepository.findByUsuarioIdAndLeidaFalse(usuarioId);
    }

    public Notificacion crearNotificacion(Notificacion notificacion) {
        // Validación inicial del estado
        if (notificacion.getEstado() == null) {
            notificacion.setEstado("PENDIENTE");
        }

        Notificacion guardada = null;
        try {
            guardada = notificacionRepository.save(notificacion);
            log.info("Notificación guardada exitosamente con ID: {}", guardada.getId());
            
            // Emitir la notificación por SSE al usuario en tiempo real
            sseNotificationService.sendNotification(guardada.getUsuarioId(), guardada);

        } catch (Exception e) {
            log.error("Error al guardar la notificación en base de datos", e);
            throw e;
        }

        // Si es de tipo EMAIL o AMBOS, intentamos enviar correo
        if ("EMAIL".equalsIgnoreCase(guardada.getTipo()) || "AMBOS".equalsIgnoreCase(guardada.getTipo())) {
            enviarNotificacionPorCorreo(guardada);
        }

        return guardada;
    }

    private void enviarNotificacionPorCorreo(Notificacion notificacion) {
        try {
            log.info("Iniciando envío de correo para la notificación ID: {}", notificacion.getId());

            String email;
            Optional<Usuario> optUsuario = usuarioRepository.findById(notificacion.getUsuarioId());
            if (optUsuario.isEmpty()) {
                // Posible lazy provisioning al vuelo si la ID es un correo válido
                if (notificacion.getUsuarioId().contains("@")) {
                    email = notificacion.getUsuarioId();
                    log.info("Usuario local {} no encontrado, pero es un correo válido. Auto-creando registro para notificaciones...", email);
                    Usuario nuevo = new Usuario();
                    nuevo.setId(email);
                    nuevo.setEmail(email);
                    nuevo.setNombre(email.split("@")[0]);
                    nuevo.setPassword("EXTERNAL_USER");
                    usuarioRepository.save(nuevo);
                } else {
                    log.warn("No se pudo enviar correo: Usuario con ID {} no encontrado", notificacion.getUsuarioId());
                    notificacion.setEstado("FALLIDO");
                    notificacionRepository.save(notificacion);
                    return;
                }
            } else {
                email = optUsuario.get().getEmail();
            }

            if (email == null || email.isBlank()) {
                log.warn("El usuario ID {} no tiene un correo configurado", notificacion.getUsuarioId());
                notificacion.setEstado("FALLIDO");
                notificacionRepository.save(notificacion);
                return;
            }

            // En este caso usamos el envio genérico
            String asunto = notificacion.getTitulo() != null ? notificacion.getTitulo()
                    : "Nueva notificación de Sanity";

            // Un poco de formato HTML genérico
            String html = """
                    <html>
                    <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;">
                        <div style="max-width: 600px; margin: auto; background: white; border-radius: 10px; padding: 30px;">
                            <h2 style="color: #6C63FF;">%s</h2>
                            <p>%s</p>
                            <p style="margin-top: 20px; color: #888; font-size: 12px;">
                                — El equipo de Sanity
                            </p>
                        </div>
                    </body>
                    </html>
                    """
                    .formatted(asunto, notificacion.getMensaje());

            emailService.enviarEmail(email, asunto, html);

            notificacion.setEstado("ENVIADO");
            notificacionRepository.save(notificacion);
            log.info("Correo enviado exitosamente para notificación ID: {} al correo: {}", notificacion.getId(), email);

        } catch (Exception e) {
            log.error("Fallo al enviar correo para la notificación ID: {}", notificacion.getId(), e);
            notificacion.setEstado("FALLIDO");
            notificacionRepository.save(notificacion);
        }
    }

    public Notificacion marcarComoLeida(Integer id) {
        Optional<Notificacion> opt = notificacionRepository.findById(id);
        if (opt.isPresent()) {
            Notificacion n = opt.get();
            n.setLeida(true);
            n.setEstado("LEIDO");
            return notificacionRepository.save(n);
        }
        return null;
    }

    public void eliminarNotificacion(Integer id) {
        if (notificacionRepository.existsById(id)) {
            notificacionRepository.deleteById(id);
        } else {
            throw new IllegalArgumentException("Notificación no encontrada con código " + id);
        }
    }
}