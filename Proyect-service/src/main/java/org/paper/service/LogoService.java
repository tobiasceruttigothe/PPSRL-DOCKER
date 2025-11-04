package org.paper.service;

import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.paper.dtoCreate.LogoCreateDto;
import org.paper.dtoCreate.LogoUpdateDto;
import org.paper.dtoResponse.LogoResponseDto;
import org.paper.entity.Logo;
import org.paper.entity.Usuario;
import org.paper.exception.EntityNotFoundException;
import org.paper.exception.UnauthorizedAccessException;
import org.paper.repository.LogoRepository;
import org.paper.repository.UsuarioRepository;
import org.paper.util.Base64ValidatorUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class LogoService {

    private final LogoRepository logoRepository;
    private final UsuarioRepository usuarioRepository;
    private final Base64ValidatorUtil base64Validator;

    public LogoService(LogoRepository logoRepository,
                       UsuarioRepository usuarioRepository,
                       Base64ValidatorUtil base64Validator) {
        this.logoRepository = logoRepository;
        this.usuarioRepository = usuarioRepository;
        this.base64Validator = base64Validator;
    }

    /**
     * Crea un nuevo logo para un usuario
     */
    @Transactional
    public LogoResponseDto crearLogo(LogoCreateDto dto) {
        log.info("Iniciando creación de logo '{}' para usuario: {}", dto.getNombre(), dto.getUsuarioId());

        // 1. Validar que el usuario exista
        Usuario usuario = usuarioRepository.findById(dto.getUsuarioId())
                .orElseThrow(() -> {
                    log.error("Usuario no encontrado: {}", dto.getUsuarioId());
                    return new EntityNotFoundException("Usuario", dto.getUsuarioId());
                });

        // 2. Validar el base64 usando la utilidad
        base64Validator.validateBase64ForLogo(dto.getBase64Logo(), dto.getNombre());

        // 3. Calcular tamaño usando la utilidad
        long tamanoBytes = base64Validator.calculateBase64Size(dto.getBase64Logo());

        // 4. Crear entidad
        Logo logo = new Logo();
        logo.setUsuario(usuario);
        logo.setNombre(dto.getNombre());
        logo.setBase64Logo(dto.getBase64Logo());
        logo.setFechaCreacion(LocalDateTime.now());
        logo.setTamanoBytes(tamanoBytes);

        // 5. Guardar
        Logo savedLogo = logoRepository.save(logo);

        log.info("Logo creado exitosamente con ID: {} para usuario: {}",
                savedLogo.getId(), dto.getUsuarioId());

        return mapToResponseDto(savedLogo);
    }

    /**
     * Obtiene todos los logos de un usuario
     */
    @Transactional(readOnly = true)
    public List<LogoResponseDto> obtenerLogosPorUsuario(UUID usuarioId) {
        log.debug("Obteniendo logos para usuario: {}", usuarioId);

        // Validar que el usuario exista
        if (!usuarioRepository.existsById(usuarioId)) {
            log.error("Usuario no encontrado: {}", usuarioId);
            throw new EntityNotFoundException("Usuario", usuarioId);
        }

        List<Logo> logos = logoRepository.findByUsuarioId(usuarioId);

        log.info("Se encontraron {} logos para el usuario: {}", logos.size(), usuarioId);

        return logos.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene un logo específico por ID
     */
    @Transactional(readOnly = true)
    public LogoResponseDto obtenerLogoPorId(Integer logoId) {
        log.debug("Obteniendo logo con ID: {}", logoId);

        Logo logo = logoRepository.findById(logoId)
                .orElseThrow(() -> {
                    log.error("Logo no encontrado: {}", logoId);
                    return new EntityNotFoundException("Logo", logoId);
                });

        log.debug("Logo encontrado: {}", logoId);
        return mapToResponseDto(logo);
    }

    /**
     * Actualiza un logo existente
     */
    @Transactional
    public LogoResponseDto actualizarLogo(Integer logoId, LogoUpdateDto dto) {
        log.info("Iniciando actualización de logo con ID: {}", logoId);

        // 1. Buscar el logo
        Logo logo = logoRepository.findById(logoId)
                .orElseThrow(() -> {
                    log.error("Logo no encontrado: {}", logoId);
                    return new EntityNotFoundException("Logo", logoId);
                });

        // 2. Actualizar nombre
        logo.setNombre(dto.getNombre());

        // 3. Si viene nueva imagen, actualizar usando la utilidad
        if (dto.getBase64Logo() != null && !dto.getBase64Logo().isEmpty()) {
            base64Validator.validateBase64ForLogo(dto.getBase64Logo(), dto.getNombre());
            logo.setBase64Logo(dto.getBase64Logo());
            logo.setTamanoBytes(base64Validator.calculateBase64Size(dto.getBase64Logo()));
            log.debug("Imagen del logo actualizada");
        }

        // 4. Guardar
        Logo updatedLogo = logoRepository.save(logo);

        log.info("Logo actualizado exitosamente: {}", logoId);

        return mapToResponseDto(updatedLogo);
    }

    /**
     * Elimina un logo
     */
    @Transactional
    public void eliminarLogo(Integer logoId) {
        log.info("Iniciando eliminación de logo con ID: {}", logoId);

        // Verificar que existe
        if (!logoRepository.existsById(logoId)) {
            log.error("Logo no encontrado: {}", logoId);
            throw new EntityNotFoundException("Logo", logoId);
        }

        logoRepository.deleteById(logoId);

        log.info("Logo eliminado exitosamente: {}", logoId);
    }

    /**
     * Elimina un logo específico de un usuario
     */
    @Transactional
    public void eliminarLogoPorUsuario(UUID usuarioId, Integer logoId) {
        log.info("Eliminando logo {} del usuario {}", logoId, usuarioId);

        Logo logo = logoRepository.findById(logoId)
                .orElseThrow(() -> {
                    log.error("Logo no encontrado: {}", logoId);
                    return new EntityNotFoundException("Logo", logoId);
                });

        // Validar que el logo pertenece al usuario
        if (!logo.getUsuario().getId().equals(usuarioId)) {
            log.error("El logo {} no pertenece al usuario {}", logoId, usuarioId);
            throw new UnauthorizedAccessException(
                    usuarioId.toString(),
                    "Logo con ID " + logoId
            );
        }

        logoRepository.delete(logo);

        log.info("Logo {} eliminado exitosamente del usuario {}", logoId, usuarioId);
    }

    /**
     * Obtiene la cantidad de logos de un usuario
     */
    public long contarLogosPorUsuario(UUID usuarioId) {
        log.debug("Contando logos del usuario: {}", usuarioId);
        return logoRepository.countByUsuarioId(usuarioId);
    }

    /**
     * Mapea entidad a DTO de respuesta
     */
    private LogoResponseDto mapToResponseDto(Logo logo) {
        return LogoResponseDto.builder()
                .id(logo.getId())
                .nombre(logo.getNombre())
                .base64Logo(logo.getBase64Logo())
                .build();
    }
}