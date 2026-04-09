package com.example.microservicionotificaciones;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MicroservicioNotificacionesApplication {

	public static void main(String[] args) {
		SpringApplication.run(MicroservicioNotificacionesApplication.class, args);
	}

}
