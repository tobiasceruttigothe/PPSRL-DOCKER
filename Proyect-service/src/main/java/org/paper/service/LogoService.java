package org.paper.service;


import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.paper.dtoCreate.LogoCreateDto;
import org.paper.entity.Logo;
import org.paper.repository.LogoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@Slf4j
@Service
public class LogoService {
    private final LogoRepository logoRepository;
    private final UsuarioService usuarioService;

    public LogoService(LogoRepository logoRepository, UsuarioService usuarioService) {
        this.logoRepository = logoRepository;
        this.usuarioService = usuarioService;
    }

    @Transactional
    public ResponseEntity<?> crearLogo(LogoCreateDto logoCreateDto) {
        try {
            log.info("Iniciando creación de logo para usuario: {}", logoCreateDto.getUsuarioId());

            Logo logo = new Logo();
            logo.setUsuario(usuarioService.findbyUUID(logoCreateDto.getUsuarioId()));
            logo.setNombre(logoCreateDto.getNombre());
            logo.setBase64Logo(logoCreateDto.getBase64Logo());
            logo.setFechaCreacion(LocalDateTime.now());

            logoRepository.save(logo);

            log.info("Logo creado exitosamente para usuario: {}", logoCreateDto.getUsuarioId());
            return ResponseEntity.ok(Map.of("mensaje", "Logo creado exitosamente"));
        } catch (Exception e) {
            log.error("Error al crear el logo: {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "No se pudo crear el logo"));
        }
    }

    @Transactional
    public ResponseEntity<?> obtenerLogoPorUsuarioId(UUID usuarioId) {
        log.info("Obteniendo logo para el usuario con ID: {}", usuarioId);
        List<Logo> logos = logoRepository.findByUsuarioId(usuarioId);
        if (logos.isEmpty()) {
            log.warn("No se encontraron logos para el usuario con ID: {}", usuarioId);
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("mensaje", "No se encontraron logos para el usuario"));
        }
        log.info("Logos encontrados para el usuario con ID: {}: {}", usuarioId, logos.size());
        return ResponseEntity.ok(logos);
    }

    @Transactional
    public  ResponseEntity<?> eliminarLogoPorUsuarioId(UUID usuarioId, Integer logoId) {
        log.info("Eliminando logo con ID: {} para el usuario con ID: {}", logoId, usuarioId);
        List<Logo> logos = logoRepository.findByUsuarioId(usuarioId);
        Logo logoAEliminar = logos.stream()
                .filter(logo -> logo.getId().equals(logoId))
                .findFirst()
                .orElse(null);
        if (logoAEliminar == null) {
            log.warn("No se encontró el logo con ID: {} para el usuario con ID: {}", logoId, usuarioId);
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("mensaje", "No se encontró el logo para eliminar"));
        }
        logoRepository.delete(logoAEliminar);
        log.info("Logo con ID: {} eliminado para el usuario con ID: {}", logoId, usuarioId);
        return ResponseEntity.ok(Map.of("mensaje", "Logo eliminado exitosamente"));
    }
}
