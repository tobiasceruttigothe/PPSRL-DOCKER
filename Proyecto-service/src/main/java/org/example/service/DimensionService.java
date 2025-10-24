package org.example.service;

import org.example.entity.Dimension;
import java.util.List;

public interface DimensionService {
    Dimension create(Dimension dimension);
    Dimension update(Integer id, Dimension dimension);
    void delete(Integer id);
    Dimension get(Integer id);
    List<Dimension> list();

    // Compatibilidad con tests y controladores que usan otros nombres
    Dimension getById(Integer id);
    List<Dimension> getAll();
}
