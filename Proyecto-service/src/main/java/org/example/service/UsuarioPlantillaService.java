// org/example/services/UsuarioPlantillaService.java
package org.example.service;

import org.example.entity.UsuarioPlantilla;
import org.example.entity.UsuarioPlantillaId;
import java.util.List;
import java.util.UUID;

public interface UsuarioPlantillaService {
    UsuarioPlantilla create(UsuarioPlantilla up);
    void delete(UsuarioPlantillaId id);
    UsuarioPlantilla get(UsuarioPlantillaId id);
    List<UsuarioPlantilla> list();

    List<UsuarioPlantilla> findByUsuario(UUID usuarioId);
    List<UsuarioPlantilla> findByPlantilla(Integer plantillaId);
}
