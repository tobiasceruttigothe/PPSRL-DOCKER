package org.example.repository;

import org.example.entity.Plantilla;
import org.example.entity.Material;
import org.example.entity.TipoBolsa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlantillaRepository extends JpaRepository<Plantilla, Integer> {

    /**
     * Busca plantillas filtrando por material y tipo de bolsa
     */
    List<Plantilla> findByMaterialAndTipoBolsa(Material material, TipoBolsa tipoBolsa);

    /**
     * Busca plantillas creadas después de cierta fecha
     */
    List<Plantilla> findByFechaCreacionAfter(java.time.LocalDateTime fecha);
}
