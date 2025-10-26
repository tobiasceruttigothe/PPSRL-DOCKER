package org.paper.repository;

import org.paper.entity.TipoBolsa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TipoBolsaRepository extends JpaRepository<TipoBolsa, Integer> {

    /**
     * Verifica si existe un tipo de bolsa con ese nombre (ignora mayúsculas/minúsculas)
     */
    boolean existsByNombreIgnoreCase(String nombre);

    /**
     * Verifica si existe un tipo de bolsa con ese nombre excluyendo un ID específico
     * Útil para validar en actualizaciones
     */
    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Integer id);

    /**
     * Busca tipos de bolsa por nombre (búsqueda parcial, case-insensitive)
     */
    List<TipoBolsa> findByNombreContainingIgnoreCase(String nombre);
}