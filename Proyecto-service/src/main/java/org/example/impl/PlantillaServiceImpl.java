// org/example/impl/PlantillaServiceImpl.java
package org.example.impl;

import lombok.RequiredArgsConstructor;
import org.example.entity.Plantilla;
import org.example.repository.PlantillaRepository;
import org.example.service.PlantillaService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service @RequiredArgsConstructor @Transactional
public class PlantillaServiceImpl implements PlantillaService {
    private final PlantillaRepository repo;

    public Plantilla create(Plantilla p){ return repo.save(p); }

    public Plantilla update(Integer id, Plantilla p){
        Plantilla db = get(id);
        db.setNombre(p.getNombre());
        db.setMaterial(p.getMaterial());
        db.setTipoBolsa(p.getTipoBolsa());
        db.setDimension(p.getDimension());
        return repo.save(db);
    }

    public void delete(Integer id){ repo.deleteById(id); }

    @Transactional(readOnly = true)
    public Plantilla get(Integer id){
        return repo.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Plantilla no encontrada"));
    }

    @Transactional(readOnly = true)
    public List<Plantilla> list(){ return repo.findAll(); }

    @Transactional(readOnly = true)
    public List<Plantilla> createdAfter(LocalDateTime from){ return repo.findByFechaCreacionAfter(from); }
}
