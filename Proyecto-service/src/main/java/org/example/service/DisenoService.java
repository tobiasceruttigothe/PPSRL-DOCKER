// org/example/service/DisenoService.java
package org.example.service;

import org.example.entity.Diseno;
import java.util.List;
import java.util.UUID;

public interface DisenoService {
    Diseno create(Diseno d);
    Diseno update(Integer id, Diseno d);
    void delete(Integer id);
    Diseno get(Integer id);
    List<Diseno> list();

    List<Diseno> findByUsuario(UUID usuarioId);
    List<Diseno> findByPlantilla(Integer plantillaId);

    // Compatibilidad con tests/controladores que usan otros nombres
    default Diseno getById(Integer id) { return get(id); }
    default List<Diseno> getAll() { return list(); }
}
