package com.gym.service.oracle;

import com.gym.service.oracle.dto.BitacoraAccesoOracleDto;
import java.util.List;

public interface OracleBitacoraService {

    void registrarAcceso(Long idUsuario,
                         Long idSucursal,
                         String resultado,
                         String motivo);

    List<BitacoraAccesoOracleDto> listarAccesosPorUsuario(Long idUsuario);

    int contarAccesosDenegadosUsuario(Long idUsuario);
}