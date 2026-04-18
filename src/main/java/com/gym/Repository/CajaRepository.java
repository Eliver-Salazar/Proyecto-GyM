package com.gym.Repository;

import com.gym.domain.Caja;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CajaRepository extends JpaRepository<Caja, Long> {

    Optional<Caja> findBySucursalIdSucursalAndNombreIgnoreCase(Long idSucursal, String nombre);

    List<Caja> findAllByOrderByIdCajaDesc();
}