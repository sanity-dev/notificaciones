package com.example.microservicionotificaciones.controladores;

import com.example.microservicionotificaciones.modelos.TokenRecuperacion;
import com.example.microservicionotificaciones.repositorios.TokenRecuperacionRepository;
import com.example.microservicionotificaciones.servicios.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/recovery")
public class RecuperacionController {

    @Autowired
    private TokenRecuperacionRepository tokenRecuperacionRepository;

    @Autowired
    private EmailService emailService;
    
    @Autowired
    private RestTemplate restTemplate;

    @Value("${app.api-gateway.url}")
    private String apiGatewayUrl;

    /**
     * Solicitar recuperación de contraseña.
     * Acepta { "correo": "..." } (frontend) o { "email": "..." }
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<?> solicitarRecuperacion(@RequestBody Map<String, String> body) {
        // Aceptar tanto "correo" (frontend) como "email"
        String email = body.get("correo");
        if (email == null || email.isBlank()) {
            email = body.get("email");
        }
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El email es requerido"));
        }

        // Consultar al microservicio-usuarios a través del API Gateway
        String checkUrl = apiGatewayUrl + "/api/personas/exists?email=" + email;
        try {
            System.out.println("====== [DEBUG] Llamando a API Gateway: " + checkUrl + " ======");
            ResponseEntity<Map> response = restTemplate.getForEntity(checkUrl, Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> bodyRes = response.getBody();
            System.out.println("====== [DEBUG] Respuesta del API Gateway: " + bodyRes + " ======");
            if (bodyRes == null || !Boolean.TRUE.equals(bodyRes.get("exists"))) {
                // Por seguridad, no revelamos si el email existe o no
                System.out.println("====== [DEBUG] El usuario no existe o la respuesta fue nula. ======");
                return ResponseEntity.ok(Map.of("mensaje", "Si el email existe, recibirás un correo de recuperación."));
            }
            System.out.println("====== [DEBUG] El usuario SÍ existe. Prosiguiendo a enviar correo. ======");
        } catch (Exception e) {
            // Si hay un error de conexión, preferimos no revelar detalles
            System.err.println("====== [ERROR] Error consultando usuario en gateway ======");
            e.printStackTrace();
            return ResponseEntity.ok(Map.of("mensaje", "Si el email existe, recibirás un correo de recuperación."));
        }

        // Generar token único
        String token = UUID.randomUUID().toString();

        // Guardar token en base de datos local (notificaciones)
        TokenRecuperacion tokenRecuperacion = new TokenRecuperacion();
        tokenRecuperacion.setToken(token);
        tokenRecuperacion.setEmail(email);
        tokenRecuperacion.setFechaExpiracion(LocalDateTime.now().plusHours(1)); // Expira en 1 hora
        tokenRecuperacionRepository.save(tokenRecuperacion);

        // Enviar email con el link de recuperación
        try {
            emailService.enviarEmailRecuperacion(email, token);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "No se pudo enviar el correo de recuperación."));
        }

        return ResponseEntity.ok(Map.of("mensaje", "Si el email existe, recibirás un correo de recuperación."));
    }

    /**
     * Resetear contraseña con token.
     * Recibe { "token": "...", "nuevaPassword": "..." }
     */
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetearPassword(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        String nuevaPassword = body.get("nuevaPassword");

        if (token == null || nuevaPassword == null || nuevaPassword.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Token y nueva contraseña son requeridos"));
        }

        // Buscar token válido (no usado)
        Optional<TokenRecuperacion> tokenOpt = tokenRecuperacionRepository.findByTokenAndUsadoFalse(token);
        if (tokenOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Token inválido o ya utilizado"));
        }

        TokenRecuperacion tokenRecuperacion = tokenOpt.get();

        // Verificar que no haya expirado
        if (tokenRecuperacion.getFechaExpiracion().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body(Map.of("error", "El token ha expirado"));
        }

        // Solicitar al microservicio-usuarios (vía API Gateway) que actualice la contraseña
        String updateUrl = apiGatewayUrl + "/api/personas/reset-password";
        try {
            restTemplate.put(updateUrl, Map.of(
                "email", tokenRecuperacion.getEmail(),
                "nuevaPassword", nuevaPassword
            ));
        } catch (Exception e) {
            System.err.println("Error actualizando contraseña en gateway: " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", "Error al conectarse con el servicio de usuarios para actualizar la contraseña"));
        }

        // Marcar token como usado
        tokenRecuperacion.setUsado(true);
        tokenRecuperacionRepository.save(tokenRecuperacion);

        return ResponseEntity.ok(Map.of("mensaje", "Contraseña actualizada exitosamente"));
    }
}
