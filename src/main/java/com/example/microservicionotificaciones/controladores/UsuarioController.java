package com.example.microservicionotificaciones.controladores;

import com.example.microservicionotificaciones.dto.LoginRequestDTO;
import com.example.microservicionotificaciones.dto.LoginResponseDTO;
import com.example.microservicionotificaciones.modelos.Usuario;
import com.example.microservicionotificaciones.servicios.UsuarioService;
import com.example.microservicionotificaciones.seguridad.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import com.example.microservicionotificaciones.dto.PreferenciasDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;
import java.util.Optional;

@RestController
@RequestMapping("/api/notifications/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private JwtService jwtService;

    // 1. REGISTRO (Crear cuenta) - Ruta Pública
    @PostMapping
    public ResponseEntity<Usuario> createUsuario(@RequestBody Usuario usuario) {
        Usuario nuevoUsuario = usuarioService.createUsuario(usuario);
        return new ResponseEntity<>(nuevoUsuario, HttpStatus.CREATED);
    }

    // 2. LOGIN (Obtener Token) - Ruta Pública
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO loginRequest) {
        try {
            LoginResponseDTO response = usuarioService.login(loginRequest);
            return ResponseEntity.ok(response);
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenciales inválidas");
        }
    }

    // 3. ACTUALIZAR PREFERENCIAS
    @PutMapping("/{id}/preferencias")
    public ResponseEntity<?> updatePreferencias(
            @PathVariable String id,
            @RequestBody PreferenciasDTO preferenciasDTO,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            String email = extractEmail(userEmail, authHeader);
            Usuario usuarioActualizado = usuarioService.updatePreferenciasOAutoCrear(id, email, preferenciasDTO);
            return ResponseEntity.ok(usuarioActualizado);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // 4. OBTENER PREFERENCIAS
    @GetMapping("/{id}/preferencias")
    public ResponseEntity<?> getPreferencias(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        String email = extractEmail(userEmail, authHeader);
        Usuario usuario = usuarioService.getUsuarioOAutoCrear(id, email);
        
        PreferenciasDTO dto = new PreferenciasDTO();
        dto.setPushEnabled(usuario.isPushEnabled());
        dto.setEmailEnabled(usuario.isEmailEnabled());
        dto.setRecordatoriosCitas(usuario.isRecordatoriosCitas());
        dto.setRecordatoriosActividades(usuario.isRecordatoriosActividades());
        dto.setRecordatoriosHabitos(usuario.isRecordatoriosHabitos());
        dto.setNuevasActividades(usuario.isNuevasActividades());
        dto.setMensajesIa(usuario.isMensajesIa());
        return ResponseEntity.ok(dto);
    }
    
    private String extractEmail(String headerEmail, String authHeader) {
        if (headerEmail != null && !headerEmail.isBlank()) {
            return headerEmail;
        }
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                return jwtService.extractUsername(authHeader.substring(7));
            } catch (Exception e) {
                // Ignore
            }
        }
        return null;
    }
}