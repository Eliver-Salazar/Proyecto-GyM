package com.gym.Repository;

import com.gym.domain.EstadoMembresia;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EstadoMembresiaRepository extends JpaRepository<EstadoMembresia, Long> {
    Optional<EstadoMembresia> findByNombreEstado(String nombreEstado);
}