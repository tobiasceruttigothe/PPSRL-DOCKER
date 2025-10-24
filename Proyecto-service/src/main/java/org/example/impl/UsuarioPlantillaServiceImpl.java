// org/example/impl/UsuarioPlantillaServiceImpl.java
package org.example.impl;

import lombok.RequiredArgsConstructor;
import org.example.entity.UsuarioPlantilla;
import org.example.entity.UsuarioPlantillaId;
import org.example.repository.UsuarioPlantillaRepository;
import org.example.service.UsuarioPlantillaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service @RequiredArgsConstructor @Transactional
public class UsuarioPlantillaServiceImpl implements UsuarioPlantillaService {
    private final UsuarioPlantillaRepository repo;

    public UsuarioPlantilla create(UsuarioPlantilla up){ return repo.save(up); }
    public void delete(UsuarioPlantillaId id){ repo.deleteById(id); }
    @Transactional(readOnly = true) public UsuarioPlantilla get(UsuarioPlantillaId id){ return repo.findById(id).orElse(null); }
    @Transactional(readOnly = true) public List<UsuarioPlantilla> list(){ return repo.findAll(); }
    @Transactional(readOnly = true) public List<UsuarioPlantilla> findByUsuario(UUID uid){ return repo.findByIdUsuarioId(uid); }
    @Transactional(readOnly = true) public List<UsuarioPlantilla> findByPlantilla(Integer pid){ return repo.findByIdPlantillaId(pid); }
}
