package org.example.impl;

import lombok.RequiredArgsConstructor;
import org.example.entity.Logo;
import org.example.repository.LogoRepository;
import org.example.service.LogoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class LogoServiceImpl implements org.example.service.LogoService {

    private final LogoRepository repo;

    @Override
    public Logo create(Logo l) {
        return repo.save(l);
    }

    @Override
    public void delete(Integer id) {
        repo.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Logo get(Integer id) {
        return repo.findById(id).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Logo> getAll() {
        return repo.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Logo> getByUsuario(UUID usuarioId) {
        return repo.findByUsuarioId(usuarioId);
    }
}
