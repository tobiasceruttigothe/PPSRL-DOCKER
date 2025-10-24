package org.example.repository;

import org.example.entity.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MaterialRepository extends JpaRepository<Material, Integer> {

    /**
     * Busca material por nombre exacto
     */
    Material findByNombre(String nombre);
}
