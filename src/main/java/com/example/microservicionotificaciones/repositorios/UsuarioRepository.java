package com.example.microservicionotificaciones.repositorios;

import com.example.microservicionotificaciones.modelos.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, String> {
    // Método mágico para encontrar usuario por email (Necesario para el Login)
    Optional<Usuario> findByEmail(String email);
}