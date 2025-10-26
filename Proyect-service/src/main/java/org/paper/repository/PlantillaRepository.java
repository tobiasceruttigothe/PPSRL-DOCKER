package org.paper.repository;

import org.paper.entity.Plantilla;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PlantillaRepository extends JpaRepository<Plantilla, Integer> {

    /**
     * Busca plantillas por material
     */
    List<Plantilla> findByMaterialId(Integer materialId);

    /**
     * Busca plantillas por tipo de bolsa
     */
    List<Plantilla> findByTipoBolsaId(Integer tipoBolsaId);

    /**
     * Busca plantillas por nombre (búsqueda parcial, case-insensitive)
     */
    List<Plantilla> findByNombreContainingIgnoreCase(String nombre);

    /**
     * Busca plantillas habilitadas para un usuario específico
     */
    List<Plantilla> findByUsuariosHabilitadosId(UUID usuarioId);
}