package com.gym.Repository;

import com.gym.domain.Membresia;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembresiaRepository extends JpaRepository<Membresia, Long> {

    Optional<Membresia> findByUsuarioIdUsuario(Long idUsuario);

    boolean existsByUsuarioIdUsuario(Long idUsuario);
}