package org.example.repository;

import org.example.entity.TipoBolsa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TipoBolsaRepository extends JpaRepository<TipoBolsa, Integer> {

    /**
     * Busca tipo de bolsa por nombre exacto
     */
    TipoBolsa findByNombre(String nombre);
}
