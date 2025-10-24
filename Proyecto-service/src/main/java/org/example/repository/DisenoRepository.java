package org.example.repository;

import org.example.entity.Diseno;
import org.example.entity.DisenoStatus;
import org.example.entity.Plantilla;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DisenoRepository extends JpaRepository<Diseno, Integer> {

    /**
     * Busca diseños por usuario
     */
    List<Diseno> findByUsuarioId(UUID usuarioId);

    /**
     * Busca diseños de un usuario y estado específico
     */
    List<Diseno> findByUsuarioIdAndStatus(UUID usuarioId, DisenoStatus status);

    /**
     * Busca todos los diseños asociados a una plantilla
     */
    List<Diseno> findByPlantilla(Plantilla plantilla);

    /**
     * Busca los diseños más recientes de un usuario
     */
    List<Diseno> findByUsuarioIdOrderByFechaCreacionDesc(UUID usuarioId);
}
