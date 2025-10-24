// org/example/impl/ImagenServiceImpl.java
package org.example.impl;

import lombok.RequiredArgsConstructor;
import org.example.entity.Imagen;
import org.example.repository.ImagenRepository;
import org.example.service.ImagenService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service @RequiredArgsConstructor @Transactional
public class ImagenServiceImpl implements ImagenService {
    private final ImagenRepository repo;

    public Imagen create(Imagen i){ return repo.save(i); }
    @Transactional(readOnly = true) public Imagen get(Integer id){ return repo.findById(id).orElse(null); }
    @Transactional(readOnly = true) public List<Imagen> list(){ return repo.findAll(); }
    public void delete(Integer id){ repo.deleteById(id); }
    @Transactional(readOnly = true) public List<Imagen> findByFormato(String f){ return repo.findByFormato(f); }
    public void deleteOlderThan(LocalDateTime date){ repo.deleteByFechaCreacionBefore(date); }
}
