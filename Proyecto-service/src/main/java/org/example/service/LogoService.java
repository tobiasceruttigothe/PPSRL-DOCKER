package org.example.service;

import org.example.entity.Logo;
import java.util.List;
import java.util.UUID;

public interface LogoService {
    Logo create(Logo l);
    void delete(Integer id);
    Logo get(Integer id);
    List<Logo> getAll();
    List<Logo> getByUsuario(UUID usuarioId);
}
