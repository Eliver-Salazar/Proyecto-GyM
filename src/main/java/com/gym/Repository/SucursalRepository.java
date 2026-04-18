package com.gym.Repository;

import com.gym.domain.Sucursal;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SucursalRepository extends JpaRepository<Sucursal, Long> {

    Optional<Sucursal> findByNombre(String nombre);

    Optional<Sucursal> findByNombreIgnoreCase(String nombre);
}