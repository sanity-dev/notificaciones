package com.example.microservicionotificaciones.controladores;

import com.example.microservicionotificaciones.modelos.TokenRecuperacion;
import com.example.microservicionotificaciones.modelos.Usuario;
import com.example.microservicionotificaciones.repositorios.TokenRecuperacionRepository;
import com.example.microservicionotificaciones.repositorios.UsuarioRepository;
import com.example.microservicionotificaciones.servicios.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class RecuperacionController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TokenRecuperacionRepository tokenRecuperacionRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Solicitar recuperación de contraseña.
     * Recibe { "email": "usuario@ejemplo.com" }
     */
    @PostMapping("/recuperar")
    public ResponseEntity<?> solicitarRecuperacion(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El email es requerido"));
        }

        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);
        if (usuarioOpt.isEmpty()) {
            // Por seguridad, no revelamos si el email existe o no
            return ResponseEntity.ok(Map.of("mensaje", "Si el email existe, recibirás un correo de recuperación."));
        }

        // Generar token único
        String token = UUID.randomUUID().toString();

        // Guardar token en base de datos
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
    @PostMapping("/resetear")
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

        // Buscar usuario por email
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(tokenRecuperacion.getEmail());
        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Usuario no encontrado"));
        }

        // Actualizar contraseña
        Usuario usuario = usuarioOpt.get();
        usuario.setPassword(passwordEncoder.encode(nuevaPassword));
        usuarioRepository.save(usuario);

        // Marcar token como usado
        tokenRecuperacion.setUsado(true);
        tokenRecuperacionRepository.save(tokenRecuperacion);

        return ResponseEntity.ok(Map.of("mensaje", "Contraseña actualizada exitosamente"));
    }
}
