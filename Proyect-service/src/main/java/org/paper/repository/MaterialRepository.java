package org.paper.repository;

import org.paper.entity.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaterialRepository extends JpaRepository<Material, Integer> {

    /**
     * Verifica si existe un material con ese nombre (ignora mayúsculas/minúsculas)
     */
    boolean existsByNombreIgnoreCase(String nombre);

    /**
     * Verifica si existe un material con ese nombre excluyendo un ID específico
     * Útil para validar en actualizaciones
     */
    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Integer id);

    /**
     * Busca materiales por nombre (búsqueda parcial, case-insensitive)
     */
    List<Material> findByNombreContainingIgnoreCase(String nombre);
}