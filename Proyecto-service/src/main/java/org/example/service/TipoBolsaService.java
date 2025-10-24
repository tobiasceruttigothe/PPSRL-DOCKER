// org/example/services/TipoBolsaService.java
package org.example.service;

import org.example.entity.TipoBolsa;
import java.util.List;

public interface TipoBolsaService {
    TipoBolsa create(TipoBolsa t);
    TipoBolsa update(Integer id, TipoBolsa t);
    void delete(Integer id);
    TipoBolsa get(Integer id);
    List<TipoBolsa> list();
}
