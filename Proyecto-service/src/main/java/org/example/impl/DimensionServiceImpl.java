// org/example/impl/DimensionServiceImpl.java
package org.example.impl;

import lombok.RequiredArgsConstructor;
import org.example.entity.Dimension;
import org.example.repository.DimensionRepository;
import org.example.service.DimensionService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service @RequiredArgsConstructor @Transactional
public class DimensionServiceImpl implements DimensionService {
    private final DimensionRepository repo;

    public Dimension create(Dimension d){ return repo.save(d); }

    public Dimension update(Integer id, Dimension d){
        Dimension db = get(id);
        db.setAnchoMm(d.getAnchoMm());
        db.setAltoMm(d.getAltoMm());
        return repo.save(db);
    }

    public void delete(Integer id){ repo.deleteById(id); }

    @Transactional(readOnly = true)
    public Dimension get(Integer id){
        return repo.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Dimension no encontrada"));
    }

    @Transactional(readOnly = true)
    public List<Dimension> list(){ return repo.findAll(); }

    // Métodos para compatibilidad con nombres usados en tests/controladores
    @Override
    public Dimension getById(Integer id) { return get(id); }

    @Override
    public List<Dimension> getAll() { return list(); }
}
