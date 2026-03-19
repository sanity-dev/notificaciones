package com.example.microservicionotificaciones.controladores;

import com.example.microservicionotificaciones.dto.LoginRequestDTO;
import com.example.microservicionotificaciones.dto.LoginResponseDTO;
import com.example.microservicionotificaciones.modelos.Usuario;
import com.example.microservicionotificaciones.servicios.UsuarioService;
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
    public ResponseEntity<?> updatePreferencias(@PathVariable String id, @RequestBody PreferenciasDTO preferenciasDTO) {
        try {
            Usuario usuarioActualizado = usuarioService.updatePreferencias(id, preferenciasDTO);
            return ResponseEntity.ok(usuarioActualizado);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // 4. OBTENER PREFERENCIAS
    @GetMapping("/{id}/preferencias")
    public ResponseEntity<?> getPreferencias(@PathVariable String id) {
        Optional<Usuario> optionalUsuario = usuarioService.getUsuarioById(id);
        if (optionalUsuario.isPresent()) {
            Usuario usuario = optionalUsuario.get();
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
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuario no encontrado");
    }
}