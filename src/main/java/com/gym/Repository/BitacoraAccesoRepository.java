package com.gym.Repository;

import com.gym.domain.BitacoraAcceso;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BitacoraAccesoRepository extends JpaRepository<BitacoraAcceso, Long> {

    List<BitacoraAcceso> findByUsuarioIdUsuarioOrderByFechaHoraDesc(Long idUsuario);
}