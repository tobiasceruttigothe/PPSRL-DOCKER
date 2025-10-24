package org.example.repository;

import org.example.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    /**
     * Busca todos los usuarios con un estado específico.
     */
    List<Usuario> findByStatus(String status);

    /**
     * Busca usuarios en estado PENDING que aún no hayan tenido intentos.
     */
    List<Usuario> findByStatusAndUltimoIntentoIsNull(String status);

    /**
     * Busca usuarios por estado y ordena por fecha de registro descendente.
     */
    List<Usuario> findByStatusOrderByFechaRegistroDesc(String status);

    /**
     * Busca usuarios que han fallado más de cierta cantidad de intentos.
     */
    List<Usuario> findByIntentosActivacionGreaterThan(Integer maxIntentos);

    /**
     * Busca usuarios registrados después de una fecha específica.
     */
    List<Usuario> findByFechaRegistroAfter(java.time.OffsetDateTime fecha);
}
