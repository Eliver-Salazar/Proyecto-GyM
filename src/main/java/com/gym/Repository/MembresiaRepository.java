package com.gym.Repository;

import com.gym.domain.Membresia;
import com.gym.domain.Usuario;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembresiaRepository extends JpaRepository<Membresia, Long> {

    List<Membresia> findByUsuario(Usuario usuario);

    Optional<Membresia> findFirstByUsuarioOrderByFechaVencimientoDesc(Usuario usuario);
}