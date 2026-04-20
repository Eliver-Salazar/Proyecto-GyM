package com.gym.service.oracle;

import com.gym.service.oracle.dto.BitacoraAccesoOracleDto;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import oracle.jdbc.OracleTypes;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OracleBitacoraServiceImpl implements OracleBitacoraService {

    private final SimpleJdbcCall prRegistrarAcceso;
    private final SimpleJdbcCall prListarAccesosPorUsuario;
    private final SimpleJdbcCall fnContarAccesosDenegadosUsuario;

    public OracleBitacoraServiceImpl(DataSource dataSource) {
        this.prRegistrarAcceso = new SimpleJdbcCall(dataSource)
                .withCatalogName("PKG_BITACORA")
                .withProcedureName("PR_REGISTRAR_ACCESO")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("P_ID_USUARIO", Types.NUMERIC),
                        new SqlParameter("P_ID_SUCURSAL", Types.NUMERIC),
                        new SqlParameter("P_RESULTADO", Types.VARCHAR),
                        new SqlParameter("P_MOTIVO", Types.VARCHAR)
                );

        this.prListarAccesosPorUsuario = new SimpleJdbcCall(dataSource)
                .withCatalogName("PKG_BITACORA")
                .withProcedureName("PR_LISTAR_ACCESOS_POR_USUARIO")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("P_ID_USUARIO", Types.NUMERIC),
                        new SqlOutParameter("P_CURSOR", OracleTypes.CURSOR, new BitacoraAccesoOracleRowMapper())
                );

        this.fnContarAccesosDenegadosUsuario = new SimpleJdbcCall(dataSource)
                .withCatalogName("PKG_BITACORA")
                .withFunctionName("FN_CONTAR_ACCESOS_DENEGADOS_USUARIO")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("P_ID_USUARIO", Types.NUMERIC),
                        new SqlOutParameter("return", Types.NUMERIC)
                );
    }

    @Override
    @Transactional
    public void registrarAcceso(Long idUsuario,
                                Long idSucursal,
                                String resultado,
                                String motivo) {
        try {
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("P_ID_USUARIO", idUsuario)
                    .addValue("P_ID_SUCURSAL", idSucursal)
                    .addValue("P_RESULTADO", resultado)
                    .addValue("P_MOTIVO", motivo);

            prRegistrarAcceso.execute(params);

        } catch (DataAccessException ex) {
            throw new OraclePackageException(
                    traducirErrorOracle(ex, "No se pudo registrar el acceso en Oracle."),
                    ex
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<BitacoraAccesoOracleDto> listarAccesosPorUsuario(Long idUsuario) {
        try {
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("P_ID_USUARIO", idUsuario);

            Map<String, Object> result = prListarAccesosPorUsuario.execute(params);

            Object cursor = result.get("P_CURSOR");
            if (cursor instanceof List<?>) {
                return (List<BitacoraAccesoOracleDto>) cursor;
            }

            return Collections.emptyList();

        } catch (DataAccessException ex) {
            throw new OraclePackageException(
                    traducirErrorOracle(ex, "No se pudieron consultar los accesos del usuario desde Oracle."),
                    ex
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public int contarAccesosDenegadosUsuario(Long idUsuario) {
        try {
            Number total = fnContarAccesosDenegadosUsuario.executeFunction(Number.class, idUsuario);
            return total != null ? total.intValue() : 0;

        } catch (DataAccessException ex) {
            throw new OraclePackageException(
                    traducirErrorOracle(ex, "No se pudo consultar el total de accesos denegados desde Oracle."),
                    ex
            );
        }
    }

    private String traducirErrorOracle(DataAccessException ex, String mensajePorDefecto) {
        String detalle = obtenerDetalle(ex);

        if (detalle == null || detalle.isBlank()) {
            return mensajePorDefecto;
        }

        String upper = detalle.toUpperCase();

        if (upper.contains("ORA-20401")) return "El usuario indicado no existe.";
        if (upper.contains("ORA-20402")) return "La sucursal indicada no existe.";
        if (upper.contains("ORA-20403")) return "El resultado del acceso es obligatorio.";
        if (upper.contains("ORA-20404")) return "El resultado del acceso debe ser PERMITIDO o DENEGADO.";
        if (upper.contains("ORA-20405")) return "El acceso indicado no existe.";
        if (upper.contains("ORA-20406")) return "El usuario indicado no existe.";
        if (upper.contains("ORA-20407")) return "La sucursal indicada no existe.";
        if (upper.contains("ORA-20408")) return "El resultado del acceso es obligatorio.";
        if (upper.contains("ORA-20409")) return "El resultado del acceso debe ser PERMITIDO o DENEGADO.";
        if (upper.contains("ORA-20410")) return "El acceso indicado no existe.";
        if (upper.contains("ORA-20411")) return "El usuario indicado no existe.";

        return detalle;
    }

    private String obtenerDetalle(Throwable ex) {
        Throwable actual = ex;
        while (actual != null) {
            if (actual.getMessage() != null && !actual.getMessage().isBlank()) {
                return actual.getMessage();
            }
            actual = actual.getCause();
        }
        return null;
    }

    private static class BitacoraAccesoOracleRowMapper implements RowMapper<BitacoraAccesoOracleDto> {

        @Override
        public BitacoraAccesoOracleDto mapRow(ResultSet rs, int rowNum) throws SQLException {
            BitacoraAccesoOracleDto dto = new BitacoraAccesoOracleDto();
            dto.setIdAcceso(rs.getLong("ID_ACCESO"));
            dto.setFechaHora(rs.getTimestamp("FECHA_HORA"));
            dto.setResultado(rs.getString("RESULTADO"));
            dto.setMotivo(rs.getString("MOTIVO"));
            dto.setSucursal(rs.getString("SUCURSAL"));
            return dto;
        }
    }
}