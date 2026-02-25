package com.example.microservicionotificaciones.servicios;

import com.example.microservicionotificaciones.dto.LoginRequestDTO;
import com.example.microservicionotificaciones.dto.LoginResponseDTO;
import com.example.microservicionotificaciones.modelos.Usuario;
import com.example.microservicionotificaciones.repositorios.UsuarioRepository;
import com.example.microservicionotificaciones.seguridad.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    @Lazy // Usamos Lazy para evitar problemas de dependencias circulares
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    // --- REGISTRAR USUARIO ---
    public Usuario createUsuario(Usuario usuario) {
        // Encriptamos la contraseña antes de guardar
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        return usuarioRepository.save(usuario);
    }

    // --- LOGIN (Generar Token) ---
    public LoginResponseDTO login(LoginRequestDTO loginRequest) {
        // 1. Validar credenciales (Spring Security lo hace por nosotros)
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()));

        // 2. Si pasó la autenticación, cargamos los detalles del usuario
        UserDetails userDetails = userDetailsService.loadUserByUsername(loginRequest.getEmail());

        // 3. Generamos el Token
        String token = jwtService.generateToken(userDetails);

        return new LoginResponseDTO(token);
    }

    // Método auxiliar para buscar usuario por ID
    public Optional<Usuario> getUsuarioById(java.util.UUID id) {
        return usuarioRepository.findById(id);
    }

    // Método auxiliar para listar todos (opcional)
    public List<Usuario> getAllUsuarios() {
        return usuarioRepository.findAll();
    }
}