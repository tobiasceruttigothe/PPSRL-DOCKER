package org.paper.repository;



import org.paper.entity.Logo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LogoRepository extends JpaRepository<Logo, Integer> {

    /**
     * Busca logos por usuario
     */
    List<Logo> findByUsuarioId(UUID usuarioId);

}
