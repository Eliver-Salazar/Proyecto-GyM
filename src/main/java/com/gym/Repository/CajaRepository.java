package com.gym.Repository;

import com.gym.domain.Caja;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CajaRepository extends JpaRepository<Caja, Long> {

    List<Caja> findBySucursalIdSucursal(Long idSucursal);
}