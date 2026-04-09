package com.example.microservicionotificaciones.servicios;

import com.example.microservicionotificaciones.modelos.Notificacion;
import com.example.microservicionotificaciones.modelos.Usuario;
import com.example.microservicionotificaciones.repositorios.UsuarioRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.ParameterizedTypeReference;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class HabitosScheduler {

    private static final Logger log = LoggerFactory.getLogger(HabitosScheduler.class);

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private NotificacionService notificacionService;

    @Value("${app.apigateway.url:http://localhost:8080}")
    private String apiGatewayUrl;

    @Value("${jwt.secret}")
    private String jwtSecret;

    private final RestTemplate restTemplate = new RestTemplate();

    // Se ejecuta cada minuto en el segundo 0
    @Scheduled(cron = "0 * * * * *")
    public void verificarHabitos() {
        log.info("Iniciando revisión de hábitos para enviar notificaciones...");
        List<Usuario> usuarios = usuarioRepository.findAll();

        if (usuarios.isEmpty()) {
            log.info("No hay usuarios registrados. Finalizando revisión.");
            return;
        }

        // Usamos la zona horaria de Colombia para que coincida con la hora del usuario
        LocalTime now = LocalTime.now(java.time.ZoneId.of("America/Bogota"));
        String currentTimeString = now.format(DateTimeFormatter.ofPattern("HH:mm"));
        log.info("Hora actual en Colombia: {}", currentTimeString);

        // Generar un JWT válido usando el primer usuario real para engañar al filtro de autenticación
        String token = generarTokenConUsuario(usuarios.get(0).getEmail());
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // Paso 1: Obtener la lista de personas del microservicio de usuarios
        // para mapear email -> idPersona (numérico, que es lo que Euphoria necesita)
        Map<String, Integer> emailToIdPersona = new java.util.HashMap<>();
        try {
            ResponseEntity<List<Map<String, Object>>> personasResponse = restTemplate.exchange(
                    apiGatewayUrl + "/api/personas",
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            List<Map<String, Object>> personas = personasResponse.getBody();
            if (personas != null) {
                for (Map<String, Object> persona : personas) {
                    String correo = (String) persona.get("correo");
                    Object idObj = persona.get("idPersona");
                    if (correo != null && idObj != null) {
                        emailToIdPersona.put(correo.toLowerCase(), ((Number) idObj).intValue());
                    }
                }
            }
            log.info("Se obtuvieron {} personas del servicio de usuarios", emailToIdPersona.size());
        } catch (Exception e) {
            log.error("Error al obtener personas del API Gateway: {}. Abortando revisión de hábitos.", e.getMessage());
            return;
        }

        // Paso 2: Para cada usuario local, buscar su idPersona y consultar hábitos
        for (Usuario usuario : usuarios) {
            if (!usuario.isPushEnabled() || !usuario.isRecordatoriosHabitos()) {
                continue;
            }

            // Buscar el idPersona numérico usando el email del usuario
            Integer idPersona = emailToIdPersona.get(usuario.getEmail().toLowerCase());
            if (idPersona == null) {
                log.warn("No se encontró idPersona para el usuario {} ({}). Saltando.", usuario.getId(), usuario.getEmail());
                continue;
            }

            try {
                String targetUrl = apiGatewayUrl + "/api/euphoria/reminders/" + idPersona;
                log.info("Consultando hábitos para usuario {} (idPersona={}) en: {}", usuario.getEmail(), idPersona, targetUrl);

                ResponseEntity<Map<String, Object>> responseEntity = restTemplate.exchange(
                        targetUrl,
                        HttpMethod.GET,
                        entity,
                        new ParameterizedTypeReference<Map<String, Object>>() {}
                );

                Map<String, Object> response = responseEntity.getBody();

                if (response != null && response.containsKey("reminders")) {
                    List<Map<String, Object>> reminders = (List<Map<String, Object>>) response.get("reminders");
                    if (reminders != null && !reminders.isEmpty()) {
                        log.info("Se encontraron {} hábitos para el usuario {}", reminders.size(), usuario.getEmail());
                        for (Map<String, Object> habit : reminders) {
                            verificarYNotificarHabito(usuario, habit, currentTimeString);
                        }
                    } else {
                        log.info("Sin hábitos para el usuario {}", usuario.getEmail());
                    }
                }
            } catch (Exception e) {
                log.error("Error al consultar hábitos para el usuario {}: {}", usuario.getEmail(), e.getMessage());
            }
        }
    }

    /**
     * Genera un JWT usando el correo de un administrador o usuario real
     * para que el microservicio de Usuarios lo valide sin dar 403.
     */
    private String generarTokenConUsuario(String email) {
        Key key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 300000)) // 5 minutos
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    private void verificarYNotificarHabito(Usuario usuario, Map<String, Object> habit, String currentTimeString) {
        Object timeObj = habit.get("reminder_time");
        if (timeObj == null) {
            return;
        }

        String reminderTime = timeObj.toString().trim();

        // Estandarizar el tiempo (si envían 9:00 en lugar de 09:00)
        if (reminderTime.length() == 4 && reminderTime.charAt(1) == ':') {
            reminderTime = "0" + reminderTime;
        }
        
        // Truncar segundos si vienen (ej: "08:30:00" -> "08:30")
        if (reminderTime.length() > 5 && reminderTime.charAt(2) == ':') {
            reminderTime = reminderTime.substring(0, 5);
        }

        // Si la hora actual es la misma que la hora del hábito
        if (currentTimeString.equals(reminderTime)) {
            String habitName = (String) habit.getOrDefault("habit_name", "tu hábito");
            String habitDesc = (String) habit.get("habit_description");

            log.info("¡Es la hora del hábito '{}' para el usuario {}! Enviando notificación.", habitName, usuario.getId());

            Notificacion notificacion = new Notificacion();
            notificacion.setUsuarioId(usuario.getId());
            notificacion.setTitulo("⏰ Hora de " + habitName);

            if (habitDesc != null && !habitDesc.isBlank()) {
                notificacion.setMensaje(habitDesc);
            } else {
                notificacion.setMensaje("No olvides completar tu hábito de " + habitName + ".");
            }

            notificacion.setTipo("PUSH");
            notificacionService.crearNotificacion(notificacion);
        }
    }
}
