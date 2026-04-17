package com.gym.service;

import com.gym.Repository.MembresiaRepository;
import com.gym.domain.Membresia;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MembresiaServiceImpl implements MembresiaService {

    private final MembresiaRepository membresiaRepository;

    public MembresiaServiceImpl(MembresiaRepository membresiaRepository) {
        this.membresiaRepository = membresiaRepository;
    }

    @Override
    public List<Membresia> listarMembresias() {
        return membresiaRepository.findAll();
    }

    @Override
    public Membresia obtenerPorId(Long id) {
        return membresiaRepository.findById(id).orElse(null);
    }

    @Override
    public Membresia guardar(Membresia membresia) {
        return membresiaRepository.save(membresia);
    }

    @Override
    public void eliminar(Long id) {
        membresiaRepository.deleteById(id);
    }
}