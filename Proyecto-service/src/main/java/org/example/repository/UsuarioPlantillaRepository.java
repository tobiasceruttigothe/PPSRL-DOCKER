package org.example.repository;

import org.example.entity.UsuarioPlantilla;
import org.example.entity.UsuarioPlantillaId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UsuarioPlantillaRepository extends JpaRepository<UsuarioPlantilla, UsuarioPlantillaId> {

    /**
     * Busca todas las plantillas habilitadas para un usuario
     */
    List<UsuarioPlantilla> findByIdUsuarioId(UUID usuarioId);

    /**
     * Busca todos los usuarios que tienen habilitada una plantilla
     */
    List<UsuarioPlantilla> findByIdPlantillaId(Integer plantillaId);
}
