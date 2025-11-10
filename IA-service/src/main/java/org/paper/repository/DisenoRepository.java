package org.paper.repository;

import org.paper.entity.Diseno;
import org.paper.entity.DisenoStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DisenoRepository extends JpaRepository<Diseno, Integer> {

    /**
     * Busca todos los diseños de un usuario
     */
    List<Diseno> findByUsuarioId(UUID usuarioId);

    /**
     * Busca diseños de un usuario por estado
     */
    List<Diseno> findByUsuarioIdAndStatus(UUID usuarioId, DisenoStatus status);

    /**
     * Busca diseños de una plantilla específica
     */
    List<Diseno> findByPlantillaId(Integer plantillaId);

    /**
     * Busca diseños por nombre (búsqueda parcial, case-insensitive)
     */
    List<Diseno> findByNombreContainingIgnoreCase(String nombre);

    /**
     * Cuenta los diseños de un usuario
     */
    long countByUsuarioId(UUID usuarioId);

    /**
     * Cuenta los diseños de un usuario por estado
     */
    long countByUsuarioIdAndStatus(UUID usuarioId, DisenoStatus status);
}