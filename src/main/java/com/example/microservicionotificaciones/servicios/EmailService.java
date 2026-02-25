package com.example.microservicionotificaciones.servicios;

import com.azure.communication.email.EmailClient;
import com.azure.communication.email.EmailClientBuilder;
import com.azure.communication.email.models.EmailMessage;
import com.azure.communication.email.models.EmailSendResult;
import com.azure.core.util.polling.SyncPoller;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final EmailClient emailClient;

    @Value("${azure.communication.sender-email}")
    private String senderEmail;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    public EmailService(@Value("${azure.communication.connection-string}") String connectionString) {
        this.emailClient = new EmailClientBuilder()
                .connectionString(connectionString)
                .buildClient();
    }

    /**
     * Enviar email genérico
     */
    public void enviarEmail(String destinatario, String asunto, String contenidoHtml) {
        EmailMessage emailMessage = new EmailMessage()
                .setSenderAddress(senderEmail)
                .setToRecipients(destinatario)
                .setSubject(asunto)
                .setBodyHtml(contenidoHtml);

        SyncPoller<EmailSendResult, EmailSendResult> poller = emailClient.beginSend(emailMessage);
        poller.waitForCompletion();
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
