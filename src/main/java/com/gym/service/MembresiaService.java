package com.gym.service;

import com.gym.domain.Membresia;
import java.util.List;

public interface MembresiaService {
    List<Membresia> listarMembresias();
    Membresia obtenerPorId(Long id);
    Membresia guardar(Membresia membresia);
    void eliminar(Long id);
}