package org.example.service;

import org.example.entity.Usuario;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface UsuarioService {

    Usuario create(Usuario usuario);

    Usuario update(UUID id, Usuario usuario);

    void delete(UUID id);

    Usuario getById(UUID id);

    List<Usuario> getAll();

    List<Usuario> getByStatus(String status);

    List<Usuario> getFailed();

    List<Usuario> getRegisteredAfter(OffsetDateTime fecha);
}
