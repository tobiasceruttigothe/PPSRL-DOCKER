// org/example/impl/MaterialServiceImpl.java
package org.example.impl;

import lombok.RequiredArgsConstructor;
import org.example.entity.Material;
import org.example.repository.MaterialRepository;
import org.example.service.MaterialService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service @RequiredArgsConstructor @Transactional
public class MaterialServiceImpl implements MaterialService {
    private final MaterialRepository repo;

    public Material create(Material m){ return repo.save(m); }

    public Material update(Integer id, Material m){
        Material db = get(id);
        db.setNombre(m.getNombre());
        return repo.save(db);
    }

    public void delete(Integer id){ repo.deleteById(id); }

    @Transactional(readOnly = true)
    public Material get(Integer id){
        return repo.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Material no encontrado"));
    }

    @Transactional(readOnly = true)
    public List<Material> list(){ return repo.findAll(); }
}
