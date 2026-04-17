package com.gym.Repository;

import com.gym.domain.MetodoPago;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MetodoPagoRepository extends JpaRepository<MetodoPago, Long> {

    Optional<MetodoPago> findByTipoTransferencia(String tipoTransferencia);
}