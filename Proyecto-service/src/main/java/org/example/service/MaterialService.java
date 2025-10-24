// org/example/services/MaterialService.java
package org.example.service;

import org.example.entity.Material;
import java.util.List;

public interface MaterialService {
    Material create(Material m);
    Material update(Integer id, Material m);
    void delete(Integer id);
    Material get(Integer id);
    List<Material> list();
}
