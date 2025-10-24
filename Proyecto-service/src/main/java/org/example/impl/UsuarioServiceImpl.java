package org.example.impl;
import lombok.RequiredArgsConstructor;
import org.example.entity.Usuario;
import org.example.repository.UsuarioRepository;
import org.example.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository repo;

    @Override
    public Usuario create(Usuario usuario) {
        if (usuario.getFechaRegistro() == null) {
            usuario.setFechaRegistro(OffsetDateTime.now());
        }
        if (usuario.getIntentosActivacion() == null) {
            usuario.setIntentosActivacion(0);
        }
        return repo.save(usuario);
    }

    @Override
    public Usuario update(UUID id, Usuario usuario) {
        Usuario db = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        // Campos que actualizamos
        db.setStatus(usuario.getStatus());
        db.setIntentosActivacion(usuario.getIntentosActivacion());
        db.setUltimoIntento(usuario.getUltimoIntento());
        db.setMotivoFallo(usuario.getMotivoFallo());
        return repo.save(db);
    }

    @Override
    public void delete(UUID id) {
        repo.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Usuario getById(UUID id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Usuario> getAll() {
        return repo.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Usuario> getByStatus(String status) {
        return repo.findByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Usuario> getFailed() {
        return repo.findByStatus("FAILED");
    }

    @Override
    @Transactional(readOnly = true)
    public List<Usuario> getRegisteredAfter(OffsetDateTime fecha) {
        return repo.findByFechaRegistroAfter(fecha);
    }
}
