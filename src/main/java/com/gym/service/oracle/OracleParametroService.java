package com.gym.service.oracle;

public interface OracleParametroService {

    void crearParametro(String clave,
                        String valor,
                        String descripcion);

    void actualizarParametro(Long idParametro,
                             String clave,
                             String valor,
                             String descripcion);

    void eliminarParametro(Long idParametro);

    String obtenerValor(String clave);

    String obtenerDescripcion(String clave);
}