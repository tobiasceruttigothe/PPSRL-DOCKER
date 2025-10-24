// org/example/impl/TipoBolsaServiceImpl.java
package org.example.impl;

import lombok.RequiredArgsConstructor;
import org.example.entity.TipoBolsa;
import org.example.repository.TipoBolsaRepository;
import org.example.service.TipoBolsaService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service @RequiredArgsConstructor @Transactional
public class TipoBolsaServiceImpl implements TipoBolsaService {
    private final TipoBolsaRepository repo;

    public TipoBolsa create(TipoBolsa t){ return repo.save(t); }

    public TipoBolsa update(Integer id, TipoBolsa t){
        TipoBolsa db = get(id);
        db.setNombre(t.getNombre());
        return repo.save(db);
    }

    public void delete(Integer id){ repo.deleteById(id); }

    @Transactional(readOnly = true)
    public TipoBolsa get(Integer id){
        return repo.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "TipoBolsa no encontrado"));
    }

    @Transactional(readOnly = true)
    public List<TipoBolsa> list(){ return repo.findAll(); }
}
