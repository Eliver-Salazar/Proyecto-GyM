package com.gym.Repository;

import com.gym.domain.TipoMembresia;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoMembresiaRepository extends JpaRepository<TipoMembresia, Long> {
    Optional<TipoMembresia> findByNombre(String nombre);
}