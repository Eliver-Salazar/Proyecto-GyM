package com.gym.Repository;

import com.gym.domain.ParametroSistema;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParametroSistemaRepository extends JpaRepository<ParametroSistema, Long> {

    List<ParametroSistema> findAllByOrderByClaveAsc();

    boolean existsByClaveIgnoreCase(String clave);

    boolean existsByClaveIgnoreCaseAndIdParametroNot(String clave, Long idParametro);

    Optional<ParametroSistema> findByClaveIgnoreCase(String clave);
}