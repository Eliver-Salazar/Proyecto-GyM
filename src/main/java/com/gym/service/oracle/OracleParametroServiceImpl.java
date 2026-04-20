package com.gym.service.oracle;

import java.sql.Types;
import javax.sql.DataSource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OracleParametroServiceImpl implements OracleParametroService {

    private final SimpleJdbcCall prCrear;
    private final SimpleJdbcCall prActualizar;
    private final SimpleJdbcCall prEliminar;
    private final SimpleJdbcCall fnValor;
    private final SimpleJdbcCall fnDescripcion;

    public OracleParametroServiceImpl(DataSource dataSource) {

        prCrear = new SimpleJdbcCall(dataSource)
                .withCatalogName("PKG_PARAMETROS")
                .withProcedureName("PR_CREAR_PARAMETRO");

        prActualizar = new SimpleJdbcCall(dataSource)
                .withCatalogName("PKG_PARAMETROS")
                .withProcedureName("PR_ACTUALIZAR_PARAMETRO");

        prEliminar = new SimpleJdbcCall(dataSource)
                .withCatalogName("PKG_PARAMETROS")
                .withProcedureName("PR_ELIMINAR_PARAMETRO");

        fnValor = new SimpleJdbcCall(dataSource)
                .withCatalogName("PKG_PARAMETROS")
                .withFunctionName("FN_OBTENER_VALOR");

        fnDescripcion = new SimpleJdbcCall(dataSource)
                .withCatalogName("PKG_PARAMETROS")
                .withFunctionName("FN_OBTENER_DESCRIPCION");
    }

    @Override
    @Transactional
    public void crearParametro(String clave,
                               String valor,
                               String descripcion) {

        try {
            prCrear.execute(new MapSqlParameterSource()
                    .addValue("P_CLAVE", clave)
                    .addValue("P_VALOR", valor)
                    .addValue("P_DESCRIPCION", descripcion));

        } catch (DataAccessException ex) {
            throw new OraclePackageException("No se pudo crear el parámetro.", ex);
        }
    }

    @Override
    @Transactional
    public void actualizarParametro(Long idParametro,
                                    String clave,
                                    String valor,
                                    String descripcion) {

        try {
            prActualizar.execute(new MapSqlParameterSource()
                    .addValue("P_ID_PARAMETRO", idParametro)
                    .addValue("P_CLAVE", clave)
                    .addValue("P_VALOR", valor)
                    .addValue("P_DESCRIPCION", descripcion));

        } catch (DataAccessException ex) {
            throw new OraclePackageException("No se pudo actualizar el parámetro.", ex);
        }
    }

    @Override
    @Transactional
    public void eliminarParametro(Long idParametro) {

        try {
            prEliminar.execute(new MapSqlParameterSource()
                    .addValue("P_ID_PARAMETRO", idParametro));

        } catch (DataAccessException ex) {
            throw new OraclePackageException("No se pudo eliminar el parámetro.", ex);
        }
    }

    @Override
    public String obtenerValor(String clave) {
        return fnValor.executeFunction(String.class, clave);
    }

    @Override
    public String obtenerDescripcion(String clave) {
        return fnDescripcion.executeFunction(String.class, clave);
    }
}