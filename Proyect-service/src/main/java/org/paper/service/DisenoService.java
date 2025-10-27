package org.paper.service;

import lombok.extern.slf4j.Slf4j;
import org.paper.dtoCreate.DisenoCreateDto;
import org.paper.dtoCreate.DisenoUpdateDto;
import org.paper.dtoResponse.DisenoResponseDto;
import org.paper.dtoResponse.DisenoSimpleDto;
import org.paper.entity.Diseno;
import org.paper.entity.DisenoStatus;
import org.paper.entity.Plantilla;
import org.paper.entity.Usuario;
import org.paper.exception.EntityNotFoundException;
import org.paper.exception.InvalidStateException;
import org.paper.exception.UnauthorizedAccessException;
import org.paper.repository.DisenoRepository;
import org.paper.repository.PlantillaRepository;
import org.paper.repository.UsuarioRepository;
import org.paper.util.Base64ValidatorUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DisenoService {

    private final DisenoRepository disenoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PlantillaRepository plantillaRepository;
    private final Base64ValidatorUtil base64Validator;

    public DisenoService(DisenoRepository disenoRepository,
                         UsuarioRepository usuarioRepository,
                         PlantillaRepository plantillaRepository,
                         Base64ValidatorUtil base64Validator) {
        this.disenoRepository = disenoRepository;
        this.usuarioRepository = usuarioRepository;
        this.plantillaRepository = plantillaRepository;
        this.base64Validator = base64Validator;
    }

    /**
     * Obtiene todos los diseños (sin base64 para optimizar)
     */
    public List<DisenoSimpleDto> findAll() {
        log.debug("Obteniendo todos los diseños");

        List<Diseno> disenos = disenoRepository.findAll();

        log.info("Se encontraron {} diseños", disenos.size());

        return disenos.stream()
                .map(this::mapToSimpleDto)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene un diseño completo por ID (con base64)
     */
    public DisenoResponseDto findById(Integer id) {
        log.debug("Obteniendo diseño con ID: {}", id);

        Diseno diseno = disenoRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Diseño no encontrado: {}", id);
                    return new EntityNotFoundException("Diseño", id);
                });

        log.debug("Diseño encontrado: {}", diseno.getNombre());
        return mapToResponseDto(diseno);
    }

    /**
     * Crea un nuevo diseño
     */
    @Transactional
    public DisenoResponseDto save(DisenoCreateDto dto) {
        log.info("Iniciando creación de diseño: {} para usuario: {}", dto.getNombre(), dto.getUsuarioId());

        // 1. Validar que el usuario exista
        Usuario usuario = usuarioRepository.findById(dto.getUsuarioId())
                .orElseThrow(() -> {
                    log.error("Usuario no encontrado: {}", dto.getUsuarioId());
                    return new EntityNotFoundException("Usuario", dto.getUsuarioId());
                });

        // 2. Validar que la plantilla exista
        Plantilla plantilla = plantillaRepository.findById(dto.getPlantillaId())
                .orElseThrow(() -> {
                    log.error("Plantilla no encontrada: {}", dto.getPlantillaId());
                    return new EntityNotFoundException("Plantilla", dto.getPlantillaId());
                });

        // 3. Validar el base64 usando la utilidad
        base64Validator.validateBase64ForPlantillaOrDiseno(dto.getBase64Diseno(), dto.getNombre());

        // 4. Crear entidad
        Diseno diseno = new Diseno();
        diseno.setUsuario(usuario);
        diseno.setPlantilla(plantilla);
        diseno.setNombre(dto.getNombre());
        diseno.setDescripcion(dto.getDescripcion());
        diseno.setBase64Diseno(dto.getBase64Diseno());
        diseno.setStatus(DisenoStatus.PROGRESO); // Estado inicial
        diseno.setFechaCreacion(LocalDateTime.now());

        // 5. Guardar
        Diseno savedDiseno = disenoRepository.save(diseno);

        log.info("Diseño creado exitosamente con ID: {} para usuario: {}",
                savedDiseno.getId(), dto.getUsuarioId());

        return mapToResponseDto(savedDiseno);
    }

    /**
     * Actualiza un diseño existente
     */
    @Transactional
    public DisenoResponseDto update(Integer id, DisenoUpdateDto dto) {
        log.info("Iniciando actualización de diseño con ID: {}", id);

        // 1. Buscar el diseño
        Diseno diseno = disenoRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Diseño no encontrado: {}", id);
                    return new EntityNotFoundException("Diseño", id);
                });

        // 2. Validar que no esté terminado (no se puede editar un diseño finalizado)
        if (diseno.getStatus() == DisenoStatus.TERMINADO) {
            log.error("No se puede actualizar un diseño en estado TERMINADO: {}", id);
            throw new InvalidStateException("Diseño", "TERMINADO", "actualizar");
        }

        // 3. Actualizar datos básicos
        diseno.setNombre(dto.getNombre());
        diseno.setDescripcion(dto.getDescripcion());
        diseno.setFechaActualizacion(LocalDateTime.now());

        // 4. Si viene nueva imagen, actualizar usando la utilidad
        if (dto.getBase64Diseno() != null && !dto.getBase64Diseno().isEmpty()) {
            base64Validator.validateBase64ForPlantillaOrDiseno(dto.getBase64Diseno(), dto.getNombre());
            diseno.setBase64Diseno(dto.getBase64Diseno());
            log.debug("Imagen del diseño actualizada");
        }

        // 5. Guardar
        Diseno updatedDiseno = disenoRepository.save(diseno);

        log.info("Diseño actualizado exitosamente: {}", id);

        return mapToResponseDto(updatedDiseno);
    }

    /**
     * Marca un diseño como terminado
     */
    @Transactional
    public DisenoResponseDto marcarComoTerminado(Integer id) {
        log.info("Marcando diseño {} como TERMINADO", id);

        Diseno diseno = disenoRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Diseño no encontrado: {}", id);
                    return new EntityNotFoundException("Diseño", id);
                });

        // Validar que esté en progreso
        if (diseno.getStatus() == DisenoStatus.TERMINADO) {
            log.warn("El diseño {} ya está en estado TERMINADO", id);
            throw new InvalidStateException("Diseño", "TERMINADO", "marcar como terminado");
        }

        diseno.setStatus(DisenoStatus.TERMINADO);
        diseno.setFechaActualizacion(LocalDateTime.now());

        Diseno updatedDiseno = disenoRepository.save(diseno);

        log.info("Diseño {} marcado como TERMINADO exitosamente", id);

        return mapToResponseDto(updatedDiseno);
    }

    /**
     * Marca un diseño como en progreso (reabrir)
     */
    @Transactional
    public DisenoResponseDto marcarComoEnProgreso(Integer id) {
        log.info("Marcando diseño {} como EN PROGRESO", id);

        Diseno diseno = disenoRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Diseño no encontrado: {}", id);
                    return new EntityNotFoundException("Diseño", id);
                });

        // Validar que esté terminado
        if (diseno.getStatus() == DisenoStatus.PROGRESO) {
            log.warn("El diseño {} ya está en estado PROGRESO", id);
            throw new InvalidStateException("Diseño", "PROGRESO", "marcar como en progreso");
        }

        diseno.setStatus(DisenoStatus.PROGRESO);
        diseno.setFechaActualizacion(LocalDateTime.now());

        Diseno updatedDiseno = disenoRepository.save(diseno);

        log.info("Diseño {} marcado como EN PROGRESO exitosamente", id);

        return mapToResponseDto(updatedDiseno);
    }

    /**
     * Elimina un diseño
     */
    @Transactional
    public void deleteById(Integer id) {
        log.info("Iniciando eliminación de diseño con ID: {}", id);

        if (!disenoRepository.existsById(id)) {
            log.error("Diseño no encontrado: {}", id);
            throw new EntityNotFoundException("Diseño", id);
        }

        disenoRepository.deleteById(id);

        log.info("Diseño eliminado exitosamente: {}", id);
    }

    /**
     * Elimina un diseño específico de un usuario
     */
    @Transactional
    public void deleteByUsuario(UUID usuarioId, Integer disenoId) {
        log.info("Eliminando diseño {} del usuario {}", disenoId, usuarioId);

        Diseno diseno = disenoRepository.findById(disenoId)
                .orElseThrow(() -> {
                    log.error("Diseño no encontrado: {}", disenoId);
                    return new EntityNotFoundException("Diseño", disenoId);
                });

        // Validar que el diseño pertenece al usuario
        if (!diseno.getUsuario().getId().equals(usuarioId)) {
            log.error("El diseño {} no pertenece al usuario {}", disenoId, usuarioId);
            throw new UnauthorizedAccessException(
                    usuarioId.toString(),
                    "Diseño con ID " + disenoId
            );
        }

        disenoRepository.delete(diseno);

        log.info("Diseño {} eliminado exitosamente del usuario {}", disenoId, usuarioId);
    }

    /**
     * Obtiene todos los diseños de un usuario (CON base64 para vista previa)
     */
    public List<DisenoResponseDto> findByUsuario(UUID usuarioId) {
        log.debug("Obteniendo diseños del usuario: {}", usuarioId);

        // Validar que el usuario exista
        if (!usuarioRepository.existsById(usuarioId)) {
            log.error("Usuario no encontrado: {}", usuarioId);
            throw new EntityNotFoundException("Usuario", usuarioId);
        }

        List<Diseno> disenos = disenoRepository.findByUsuarioId(usuarioId);

        log.info("Se encontraron {} diseños para el usuario: {}", disenos.size(), usuarioId);

        // ✅ CAMBIO: Usar mapToResponseDto (con base64) en lugar de mapToSimpleDto
        return disenos.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene diseños de un usuario por estado (CON base64 para vista previa)
     */
    public List<DisenoResponseDto> findByUsuarioAndStatus(UUID usuarioId, DisenoStatus status) {
        log.debug("Obteniendo diseños del usuario {} en estado {}", usuarioId, status);

        // Validar que el usuario exista
        if (!usuarioRepository.existsById(usuarioId)) {
            log.error("Usuario no encontrado: {}", usuarioId);
            throw new EntityNotFoundException("Usuario", usuarioId);
        }

        List<Diseno> disenos = disenoRepository.findByUsuarioIdAndStatus(usuarioId, status);

        log.info("Se encontraron {} diseños en estado {} para el usuario: {}",
                disenos.size(), status, usuarioId);

        // ✅ CAMBIO: Usar mapToResponseDto (con base64) en lugar de mapToSimpleDto
        return disenos.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene diseños de una plantilla específica
     */
    public List<DisenoSimpleDto> findByPlantilla(Integer plantillaId) {
        log.debug("Obteniendo diseños de la plantilla: {}", plantillaId);

        // Validar que la plantilla exista
        if (!plantillaRepository.existsById(plantillaId)) {
            log.error("Plantilla no encontrada: {}", plantillaId);
            throw new EntityNotFoundException("Plantilla", plantillaId);
        }

        List<Diseno> disenos = disenoRepository.findByPlantillaId(plantillaId);

        log.info("Se encontraron {} diseños para la plantilla: {}", disenos.size(), plantillaId);

        return disenos.stream()
                .map(this::mapToSimpleDto)
                .collect(Collectors.toList());
    }

    /**
     * Busca diseños por nombre (búsqueda parcial)
     */
    public List<DisenoSimpleDto> searchByNombre(String nombre) {
        log.debug("Buscando diseños que contengan: {}", nombre);

        List<Diseno> disenos = disenoRepository.findByNombreContainingIgnoreCase(nombre);

        log.info("Se encontraron {} diseños con el criterio: {}", disenos.size(), nombre);

        return disenos.stream()
                .map(this::mapToSimpleDto)
                .collect(Collectors.toList());
    }

    /**
     * Cuenta diseños de un usuario
     */
    public long contarDisenosPorUsuario(UUID usuarioId) {
        log.debug("Contando diseños del usuario: {}", usuarioId);
        return disenoRepository.countByUsuarioId(usuarioId);
    }

    /**
     * Cuenta diseños de un usuario por estado
     */
    public long contarDisenosPorUsuarioYEstado(UUID usuarioId, DisenoStatus status) {
        log.debug("Contando diseños del usuario {} en estado {}", usuarioId, status);
        return disenoRepository.countByUsuarioIdAndStatus(usuarioId, status);
    }

    // ==================== MÉTODOS PRIVADOS ====================

    /**
     * Mapea entidad a DTO de respuesta completo
     */
    private DisenoResponseDto mapToResponseDto(Diseno diseno) {
        return DisenoResponseDto.builder()
                .id(diseno.getId())
                .nombre(diseno.getNombre())
                .descripcion(diseno.getDescripcion())
                .status(diseno.getStatus().name())
                .base64Diseno(diseno.getBase64Diseno())
                .plantillaId(diseno.getPlantilla().getId())
                .plantillaNombre(diseno.getPlantilla().getNombre())
                .fechaCreacion(diseno.getFechaCreacion())
                .fechaActualizacion(diseno.getFechaActualizacion())
                .build();
    }

    /**
     * Mapea entidad a DTO simple (sin base64)
     */
    private DisenoSimpleDto mapToSimpleDto(Diseno diseno) {
        return DisenoSimpleDto.builder()
                .id(diseno.getId())
                .nombre(diseno.getNombre())
                .descripcion(diseno.getDescripcion())
                .status(diseno.getStatus().name())
                .plantillaNombre(diseno.getPlantilla().getNombre())
                .fechaCreacion(diseno.getFechaCreacion())
                .fechaActualizacion(diseno.getFechaActualizacion())
                .build();
    }
}