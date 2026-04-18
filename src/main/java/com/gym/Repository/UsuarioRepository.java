package com.gym.Repository;

import com.gym.domain.Usuario;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreo(String correo);

    boolean existsByCorreo(String correo);

    boolean existsByCedula(String cedula);

    boolean existsByCorreoAndIdUsuarioNot(String correo, Long idUsuario);

    boolean existsByCedulaAndIdUsuarioNot(String cedula, Long idUsuario);

    List<Usuario> findByRol_NombreIgnoreCaseOrderByNombreAscApellidoAsc(String nombre);
}