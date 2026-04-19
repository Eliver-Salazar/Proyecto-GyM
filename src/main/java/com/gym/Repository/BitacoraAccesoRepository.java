package com.gym.Repository;

import com.gym.domain.BitacoraAcceso;
import java.util.Date;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BitacoraAccesoRepository extends JpaRepository<BitacoraAcceso, Long> {

    // ============================
    // PERFIL DEL CLIENTE
    // ============================
    List<BitacoraAcceso> findByUsuarioIdUsuarioOrderByFechaHoraDesc(Long idUsuario);

    // ============================
    // MÓDULO ADMINISTRATIVO
    // ============================
    @Query("""
        SELECT b
        FROM BitacoraAcceso b
        LEFT JOIN b.usuario u
        LEFT JOIN b.sucursal s
        WHERE (:textoUsuario IS NULL OR :textoUsuario = '' OR
               UPPER(COALESCE(u.nombre, '')) LIKE CONCAT('%', UPPER(:textoUsuario), '%') OR
               UPPER(COALESCE(u.apellido, '')) LIKE CONCAT('%', UPPER(:textoUsuario), '%') OR
               UPPER(COALESCE(u.correo, '')) LIKE CONCAT('%', UPPER(:textoUsuario), '%'))
          AND (:idSucursal IS NULL OR s.idSucursal = :idSucursal)
          AND (:resultado IS NULL OR :resultado = '' OR
               UPPER(COALESCE(b.resultado, '')) = UPPER(:resultado))
          AND (:fechaDesde IS NULL OR b.fechaHora >= :fechaDesde)
          AND (:fechaHasta IS NULL OR b.fechaHora <= :fechaHasta)
        ORDER BY b.fechaHora DESC, b.idAcceso DESC
        """)
    List<BitacoraAcceso> filtrarBitacora(
            @Param("textoUsuario") String textoUsuario,
            @Param("idSucursal") Long idSucursal,
            @Param("resultado") String resultado,
            @Param("fechaDesde") Date fechaDesde,
            @Param("fechaHasta") Date fechaHasta
    );

    @Query("""
        SELECT DISTINCT UPPER(TRIM(b.resultado))
        FROM BitacoraAcceso b
        WHERE b.resultado IS NOT NULL
        ORDER BY UPPER(TRIM(b.resultado))
        """)
    List<String> listarResultadosDisponibles();
}