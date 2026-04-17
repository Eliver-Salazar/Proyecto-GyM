package com.gym.Repository;

import com.gym.domain.ParametroSistema;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParametroSistemaRepository extends JpaRepository<ParametroSistema, Long> {

    Optional<ParametroSistema> findByClave(String clave);
}