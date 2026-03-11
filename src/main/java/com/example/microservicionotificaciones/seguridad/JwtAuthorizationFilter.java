package com.example.microservicionotificaciones.seguridad;

import com.example.microservicionotificaciones.servicios.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthorizationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        // 1. Buscar el header "Authorization"
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // 2. Si no hay token o no empieza con "Bearer ", dejar pasar (el SecurityConfig
        // rechazará después si es necesario)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Extraer el token limpio (quitar "Bearer ")
        jwt = authHeader.substring(7);

        try {
            userEmail = jwtService.extractUsername(jwt); // Extraer email del token
        } catch (Exception e) {
            // Si el token está mal formado o expirado
            filterChain.doFilter(request, response);
            return;
        }

        // 4. Si hay email y el usuario no está autenticado todavía en el contexto
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            try {
                // Cargar datos del usuario desde la BD
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                // 5. Validar el token
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    // Crear objeto de autenticación
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities());

                    // Añadir detalles de la petición
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // 6. ESTABLECER LA AUTENTICACIÓN FINAL (Login exitoso para esta petición)
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            } catch (Exception e) {
                // Si el usuario no existe en este microservicio pero el token es válido,
                // el gateway ya lo dejó pasar. Podríamos simplemente loguear la advertencia.
                // logger.warn("Usuario autenticado no encontrado localmente: " + userEmail);
            }
        }

        // Continuar con la cadena de filtros
        filterChain.doFilter(request, response);
    }
}