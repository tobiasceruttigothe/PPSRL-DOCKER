// org/example/services/ImagenService.java
package org.example.service;

import org.example.entity.Imagen;
import java.time.LocalDateTime;
import java.util.List;

public interface ImagenService {
    Imagen create(Imagen i);
    Imagen get(Integer id);
    List<Imagen> list();
    void delete(Integer id);
    List<Imagen> findByFormato(String formato);
    void deleteOlderThan(LocalDateTime date);
}
