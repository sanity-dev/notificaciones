package com.example.microservicionotificaciones.repositorios;

import com.example.microservicionotificaciones.modelos.CitaNotificada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CitaNotificadaRepository extends JpaRepository<CitaNotificada, Integer> {
}
