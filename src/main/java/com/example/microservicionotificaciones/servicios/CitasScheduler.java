package com.example.microservicionotificaciones.servicios;

import com.example.microservicionotificaciones.modelos.CitaNotificada;
import com.example.microservicionotificaciones.modelos.Notificacion;
import com.example.microservicionotificaciones.modelos.Usuario;
import com.example.microservicionotificaciones.repositorios.CitaNotificadaRepository;
import com.example.microservicionotificaciones.repositorios.UsuarioRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class CitasScheduler {

    private static final Logger log = LoggerFactory.getLogger(CitasScheduler.class);

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private NotificacionService notificacionService;

    @Autowired
    private CitaNotificadaRepository citaNotificadaRepository;

    @Value("${app.apigateway.url}")
    private String apiGatewayUrl;

    @Value("${jwt.secret}")
    private String jwtSecret;

    private final RestTemplate restTemplate = new RestTemplate();

    @Scheduled(cron = "0 * * * * *")
    public void verificarCitas() {
        log.info("Iniciando revisión de citas para enviar notificaciones...");
        List<Usuario> usuarios = usuarioRepository.findAll();

        if (usuarios.isEmpty()) {
            log.info("No hay usuarios locales para usar como token generador.");
            return;
        }

        LocalDateTime now = LocalDateTime.now(ZoneId.of("America/Bogota"));
        
        String token = generarTokenConUsuario(usuarios.get(0).getEmail());
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        Map<Integer, String> idPersonaToEmail = new HashMap<>();
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
                        idPersonaToEmail.put(((Number) idObj).intValue(), correo);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error al obtener personas del API Gateway en CitasScheduler: {}", e.getMessage());
            return;
        }

        try {
            ResponseEntity<List<Map<String, Object>>> specialistResponse = restTemplate.exchange(
                    apiGatewayUrl + "/api/specialist",
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            
            List<Map<String, Object>> specialists = specialistResponse.getBody();
            if (specialists != null) {
                for (Map<String, Object> specialist : specialists) {
                    
                    String specialistEmail = (String) specialist.get("email");
                    if (specialistEmail == null) continue;

                    boolean notifyTherapist = true;
                    try {
                        Usuario terapeutaLocal = usuarioRepository.findByEmail(specialistEmail).orElse(null);
                        if (terapeutaLocal != null && !terapeutaLocal.isRecordatoriosCitas()) {
                            notifyTherapist = false;
                        }
                    } catch (Exception ignored) {}

                    List<Map<String, Object>> citas = (List<Map<String, Object>>) specialist.get("citas");
                    if (citas == null || citas.isEmpty()) continue;

                    for (Map<String, Object> cita : citas) {
                        Object idObj = cita.get("id");
                        if (idObj == null) continue;
                        Integer citaId = ((Number) idObj).intValue();

                        Object f = cita.get("fecha");
                        if (f == null) continue;
                        
                        String fechaStr = String.valueOf(f);
                        LocalDateTime citaDate;
                        try {
                            java.time.Instant instant = java.time.Instant.parse(fechaStr);
                            citaDate = LocalDateTime.ofInstant(instant, ZoneId.of("America/Bogota"));
                        } catch (Exception ex) {
                            continue;
                        }

                        CitaNotificada cNotificada = citaNotificadaRepository.findById(citaId).orElse(new CitaNotificada(citaId));
                        String tipoSesion = (String) cita.get("tipoSesion");

                        // Manejar el correo del paciente
                        String patientEmail = null;
                        boolean notifyPatient = true;
                        Object pacienteIDObj = cita.get("pacienteID");
                        if (pacienteIDObj != null) {
                            Integer pID = ((Number) pacienteIDObj).intValue();
                            patientEmail = idPersonaToEmail.get(pID);
                            if (patientEmail != null) {
                                Usuario pacienteLocal = usuarioRepository.findByEmail(patientEmail).orElse(null);
                                if (pacienteLocal != null && !pacienteLocal.isRecordatoriosCitas()) {
                                    notifyPatient = false;
                                }
                            }
                        }

                        boolean savedChanges = false;
                        String prettyDate = citaDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

                        // 1. Notificación de creación
                        if (!cNotificada.isCreacionNotificada()) {
                            if (notifyTherapist) {
                                Notificacion nEspec = new Notificacion();
                                nEspec.setUsuarioId(specialistEmail);
                                nEspec.setTitulo("Nueva cita programada");
                                nEspec.setMensaje("Se ha agendado una nueva sesión (" + tipoSesion + ") para el " + prettyDate);
                                nEspec.setTipo("AMBOS");
                                notificacionService.crearNotificacion(nEspec);
                            }
                            if (patientEmail != null && notifyPatient) {
                                Notificacion nPac = new Notificacion();
                                nPac.setUsuarioId(patientEmail);
                                nPac.setTitulo("Cita agendada exitosamente");
                                nPac.setMensaje("Tu sesión (" + tipoSesion + ") ha quedado confirmada para el " + prettyDate);
                                nPac.setTipo("AMBOS");
                                notificacionService.crearNotificacion(nPac);
                            }
                            cNotificada.setCreacionNotificada(true);
                            savedChanges = true;
                        }

                        // 2. Recordatorio 15 minutos antes
                        long minutosRestantes = ChronoUnit.MINUTES.between(now, citaDate);
                        if (minutosRestantes > 0 && minutosRestantes <= 15 && !cNotificada.isRecordatorioLanzado()) {
                            if (notifyTherapist) {
                                Notificacion nEspec = new Notificacion();
                                nEspec.setUsuarioId(specialistEmail);
                                nEspec.setTitulo("Recordatorio de Cita");
                                nEspec.setMensaje("Tu sesión (" + tipoSesion + ") está programada para empezar en breve (" + prettyDate + ").");
                                nEspec.setTipo("AMBOS");
                                notificacionService.crearNotificacion(nEspec);
                            }
                            if (patientEmail != null && notifyPatient) {
                                Notificacion nPac = new Notificacion();
                                nPac.setUsuarioId(patientEmail);
                                nPac.setTitulo("Recordatorio de Cita");
                                nPac.setMensaje("Tu sesión (" + tipoSesion + ") va a comenzar pronto (" + prettyDate + ").");
                                nPac.setTipo("AMBOS");
                                notificacionService.crearNotificacion(nPac);
                            }
                            cNotificada.setRecordatorioLanzado(true);
                            savedChanges = true;
                        }

                        if (savedChanges) {
                            citaNotificadaRepository.save(cNotificada);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error consultando api/specialist: {}", e.getMessage());
        }
    }

    private String generarTokenConUsuario(String email) {
        Key key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 300000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}
