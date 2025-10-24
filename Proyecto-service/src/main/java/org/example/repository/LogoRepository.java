package org.example.repository;

import org.example.entity.Logo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LogoRepository extends JpaRepository<Logo, Integer> {
    List<Logo> findByUsuarioId(UUID usuarioId);
}
