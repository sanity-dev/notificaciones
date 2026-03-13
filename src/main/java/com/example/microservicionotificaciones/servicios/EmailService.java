package com.example.microservicionotificaciones.servicios;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Enviar email genérico con contenido HTML
     */
    public void enviarEmail(String destinatario, String asunto, String contenidoHtml) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(senderEmail);
            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText(contenidoHtml, true); // true = es HTML

            mailSender.send(mimeMessage);
            log.info("Email enviado exitosamente a: {}", destinatario);
        } catch (MessagingException e) {
            log.error("Fallo al enviar correo a {}. Motivo: {}", destinatario, e.getMessage(), e);
            throw new RuntimeException("Error al enviar correo", e);
        }
    }

    /**
     * Email de bienvenida al registrarse
     */
    public void enviarEmailBienvenida(String email, String nombre) {
        String asunto = "¡Bienvenido a Sanity, " + nombre + "!";
        String html = """
                <html>
                <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;">
                    <div style="max-width: 600px; margin: auto; background: white; border-radius: 10px; padding: 30px;">
                        <h1 style="color: #6C63FF;">¡Bienvenido a Sanity!</h1>
                        <p>Hola <strong>%s</strong>,</p>
                        <p>Tu cuenta ha sido creada exitosamente. Ahora puedes empezar a usar nuestra plataforma.</p>
                        <p style="margin-top: 20px;">— El equipo de Sanity</p>
                    </div>
                </body>
                </html>
                """.formatted(nombre);

        enviarEmail(email, asunto, html);
    }

    /**
     * Email de recuperación de contraseña con token
     */
    public void enviarEmailRecuperacion(String email, String token) {
        String resetLink = frontendUrl + "/resetear-password?token=" + token;
        String asunto = "Recuperación de contraseña - Sanity";
        String html = """
                <html>
                <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;">
                    <div style="max-width: 600px; margin: auto; background: white; border-radius: 10px; padding: 30px;">
                        <h1 style="color: #6C63FF;">Recuperar Contraseña</h1>
                        <p>Hemos recibido una solicitud para restablecer tu contraseña.</p>
                        <p>Haz clic en el siguiente botón para crear una nueva contraseña:</p>
                        <a href="%s"
                           style="display: inline-block; background-color: #6C63FF; color: white; padding: 12px 24px;
                                  text-decoration: none; border-radius: 5px; margin-top: 15px;">
                            Restablecer Contraseña
                        </a>
                        <p style="margin-top: 20px; color: #888; font-size: 12px;">
                            Este enlace expira en 1 hora. Si no solicitaste este cambio, ignora este correo.
                        </p>
                    </div>
                </body>
                </html>
                """.formatted(resetLink);

        enviarEmail(email, asunto, html);
    }
}
