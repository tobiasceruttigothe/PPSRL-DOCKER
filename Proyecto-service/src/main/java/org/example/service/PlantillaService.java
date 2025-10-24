// org/example/services/PlantillaService.java
package org.example.service;

import org.example.entity.Plantilla;
import java.time.LocalDateTime;
import java.util.List;

public interface PlantillaService {
    Plantilla create(Plantilla p);
    Plantilla update(Integer id, Plantilla p);
    void delete(Integer id);
    Plantilla get(Integer id);
    List<Plantilla> list();

    List<Plantilla> createdAfter(LocalDateTime from);

    // Compatibilidad con tests/controladores
    default Plantilla getById(Integer id) { return get(id); }
    default List<Plantilla> getCreatedAfter(LocalDateTime from) { return createdAfter(from); }
    default List<Plantilla> getAll() { return list(); }
}
