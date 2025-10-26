package org.paper.repository;

import org.paper.entity.Logo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LogoRepository extends JpaRepository<Logo, Integer> {

    /**
     * Busca todos los logos de un usuario
     */
    List<Logo> findByUsuarioId(UUID usuarioId);

    /**
     * Cuenta los logos de un usuario
     */
    long countByUsuarioId(UUID usuarioId);

    /**
     * Verifica si existe un logo con un nombre específico para un usuario
     */
    boolean existsByUsuarioIdAndNombre(UUID usuarioId, String nombre);
}