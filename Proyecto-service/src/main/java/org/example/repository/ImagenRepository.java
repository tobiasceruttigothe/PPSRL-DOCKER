package org.example.repository;

import org.example.entity.Imagen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ImagenRepository extends JpaRepository<Imagen, Integer> {

    /**
     * Busca imágenes por formato (png, jpg, etc.)
     */
    List<Imagen> findByFormato(String formato);

    /**
     * Elimina imágenes más antiguas que cierta fecha
     */
    void deleteByFechaCreacionBefore(LocalDateTime fecha);
}
