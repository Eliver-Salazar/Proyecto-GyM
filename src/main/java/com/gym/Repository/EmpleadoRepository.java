package com.gym.Repository;

import com.gym.domain.Empleado;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {

    Optional<Empleado> findByUsuarioIdUsuario(Long idUsuario);

    boolean existsByUsuarioIdUsuario(Long idUsuario);
}