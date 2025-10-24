// org/example/impl/DisenoServiceImpl.java
package org.example.impl;

import lombok.RequiredArgsConstructor;
import org.example.entity.Diseno;
import org.example.entity.Plantilla;     // << IMPORTANTE
import org.example.repository.DisenoRepository;
import org.example.service.DisenoService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service @RequiredArgsConstructor @Transactional
public class DisenoServiceImpl implements DisenoService {
    private final DisenoRepository repo;

    @Override
    public Diseno create(Diseno d){ return repo.save(d); }

    @Override
    public Diseno update(Integer id, Diseno d){
        Diseno db = get(id);
        db.setUsuarioId(d.getUsuarioId());
        db.setPlantilla(d.getPlantilla());
        db.setNombre(d.getNombre());
        db.setDescripcion(d.getDescripcion());
        db.setImagen(d.getImagen());
        db.setStatus(d.getStatus());
        db.setFechaActualizacion(d.getFechaActualizacion());
        return repo.save(db);
    }

    @Override
    public void delete(Integer id){ repo.deleteById(id); }

    @Override
    @Transactional(readOnly = true)
    public Diseno get(Integer id){
        return repo.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Diseno no encontrado"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Diseno> list(){ return repo.findAll(); }

    @Override
    @Transactional(readOnly = true)
    public List<Diseno> findByUsuario(UUID usuarioId){ return repo.findByUsuarioId(usuarioId); }

    @Override
    @Transactional(readOnly = true)
    public List<Diseno> findByPlantilla(Integer plantillaId){
        Plantilla p = new Plantilla();
        p.setId(plantillaId);
        return repo.findByPlantilla(p); // << usa el finder del repo
    }

    // Métodos compatibilidad interfaz
    @Override
    public Diseno getById(Integer id) { return get(id); }

    @Override
    public List<Diseno> getAll() { return list(); }
}
