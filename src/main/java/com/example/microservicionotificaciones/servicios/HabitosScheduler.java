package com.example.microservicionotificaciones.servicios;

import com.example.microservicionotificaciones.modelos.Notificacion;
import com.example.microservicionotificaciones.modelos.Usuario;
import com.example.microservicionotificaciones.repositorios.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@Service
public class HabitosScheduler {

    private static final Logger log = LoggerFactory.getLogger(HabitosScheduler.class);

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private NotificacionService notificacionService;

    @Value("${app.api-gateway.url:http://localhost:8080}")
    private String apiGatewayUrl;
    
    // Fallback direct url in case API gateway is not reachable
    @Value("${app.euphoria.url:http://localhost:5000}")
    private String euphoriaUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    // Se ejecuta cada minuto en el segundo 0
    @Scheduled(cron = "0 * * * * *")
    public void verificarHabitos() {
        log.info("Iniciando revisión de hábitos para enviar notificaciones...");
        List<Usuario> usuarios = usuarioRepository.findAll();

        LocalTime now = LocalTime.now();
        String currentTimeString = now.format(DateTimeFormatter.ofPattern("HH:mm"));

        for (Usuario usuario : usuarios) {
            // Solo notificamos si el usuario tiene habilitadas las notificaciones de hábitos y push
            if (!usuario.isPushEnabled() || !usuario.isRecordatoriosHabitos()) {
                continue;
            }

            try {
                // Primero intentamos con el API Gateway, si falla, intentamos directo a Euphoria
                String targetUrl = apiGatewayUrl + "/api/euphoria/reminders/" + usuario.getId();
                Map<String, Object> response = null;
                try {
                    response = consultarEuphoria(targetUrl);
                } catch (Exception e) {
                    log.warn("Fallo consultando a través de API Gateway. Intentando conexión directa a Euphoria...");
                    targetUrl = euphoriaUrl + "/api/euphoria/reminders/" + usuario.getId();
                    response = consultarEuphoria(targetUrl);
                }

                if (response != null && response.containsKey("reminders")) {
                    List<Map<String, Object>> reminders = (List<Map<String, Object>>) response.get("reminders");
                    if (reminders != null) {
                        for (Map<String, Object> habit : reminders) {
                            verificarYNotificarHabito(usuario, habit, currentTimeString);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Error al consultar hábitos para el usuario {}: {}", usuario.getId(), e.getMessage());
            }
        }
    }

    private Map<String, Object> consultarEuphoria(String url) {
        ResponseEntity<Map<String, Object>> responseEntity = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );
        return responseEntity.getBody();
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

        // Si la hora actual es la misma que la hora del hábito
        if (currentTimeString.equals(reminderTime)) {
            // Además podríamos validar 'reminder_days' según el día de la semana si el array de días no está vacío.
            
            String habitName = (String) habit.getOrDefault("habit_name", "tu hábito");
            String habitDesc = (String) habit.get("habit_description");

            log.info("Es la hora del hábito '{}' para el usuario {}. Enviando notificación.", habitName, usuario.getId());

            Notificacion notificacion = new Notificacion();
            notificacion.setUsuarioId(usuario.getId());
            notificacion.setTitulo("Hora de " + habitName);
            
            if (habitDesc != null && !habitDesc.isBlank()) {
                notificacion.setMensaje(habitDesc);
            } else {
                notificacion.setMensaje("No olvides completar tu hábito de " + habitName + ".");
            }
            
            notificacion.setTipo("PUSH");
            // NotificacionService se encarga de guardar y enviar vía SSE
            notificacionService.crearNotificacion(notificacion);
        }
    }
}
