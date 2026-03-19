package com.example.microservicionotificaciones.servicios;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SseNotificationService {

    // Store active emitters by usuarioId
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String usuarioId) {
        // Create a new emitter with a timeout of 30 minutes (or longer as needed)
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        emitters.put(usuarioId, emitter);

        emitter.onCompletion(() -> emitters.remove(usuarioId));
        emitter.onTimeout(() -> emitters.remove(usuarioId));
        emitter.onError((e) -> emitters.remove(usuarioId));

        // Send a dummy event to establish the connection
        try {
            emitter.send(SseEmitter.event().name("INIT").data("Connected successfully"));
        } catch (IOException e) {
            emitters.remove(usuarioId);
        }

        return emitter;
    }

    public void sendNotification(String usuarioId, Object notification) {
        SseEmitter emitter = emitters.get(usuarioId);
        if (emitter != null) {
            try {
                // Send an event named "notification" containing the notification object as JSON
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(notification));
            } catch (IOException e) {
                // If there's an error sending, assume the connection is dead and remove it
                emitters.remove(usuarioId);
            }
        }
    }
}
