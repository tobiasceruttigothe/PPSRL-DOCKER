package org.example.repository;

import org.example.entity.Dimension;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DimensionRepository extends JpaRepository<Dimension, Integer> {

    /**
     * Busca dimensión específica por ancho y alto (en mm)
     */
    Dimension findByAnchoMmAndAltoMm(Integer anchoMm, Integer altoMm);
}
