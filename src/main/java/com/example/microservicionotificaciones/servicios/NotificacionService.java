package com.example.microservicionotificaciones.servicios;

import com.example.microservicionotificaciones.modelos.Notificacion;
import com.example.microservicionotificaciones.repositorios.NotificacionRepository;
import com.example.microservicionotificaciones.repositorios.UsuarioRepository;
import com.example.microservicionotificaciones.modelos.Usuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class NotificacionService {

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    public List<Notificacion> obtenerPorUsuario(UUID usuarioId) {
        return notificacionRepository.findByUsuarioId(usuarioId);
    }

    public Optional<Notificacion> obtenerPorId(Integer id) {
        return notificacionRepository.findById(id);
    }

    public List<Notificacion> obtenerNoLeidas(UUID usuarioId) {
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

            Optional<Usuario> optUsuario = usuarioRepository.findById(notificacion.getUsuarioId());
            if (optUsuario.isEmpty()) {
                log.warn("No se pudo enviar correo: Usuario con ID {} no encontrado", notificacion.getUsuarioId());
                notificacion.setEstado("FALLIDO");
                notificacionRepository.save(notificacion);
                return;
            }

            Usuario usuario = optUsuario.get();
            String email = usuario.getEmail();

            if (email == null || email.isBlank()) {
                log.warn("El usuario ID {} no tiene un correo configurado", usuario.getId());
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