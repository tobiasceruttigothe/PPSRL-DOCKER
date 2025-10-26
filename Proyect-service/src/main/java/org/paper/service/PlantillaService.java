package org.paper.service;

import lombok.extern.slf4j.Slf4j;
import org.paper.dtoCreate.PlantillaCreateDto;
import org.paper.dtoCreate.PlantillaUpdateDto;
import org.paper.dtoResponse.MaterialResponseDto;
import org.paper.dtoResponse.PlantillaResponseDto;
import org.paper.dtoResponse.PlantillaSimpleDto;
import org.paper.dtoResponse.TipoBolsaResponseDto;
import org.paper.entity.Material;
import org.paper.entity.Plantilla;
import org.paper.entity.TipoBolsa;
import org.paper.entity.Usuario;
import org.paper.exception.EntityNotFoundException;
import org.paper.repository.MaterialRepository;
import org.paper.repository.PlantillaRepository;
import org.paper.repository.TipoBolsaRepository;
import org.paper.repository.UsuarioRepository;
import org.paper.util.Base64ValidatorUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PlantillaService {

    private final PlantillaRepository plantillaRepository;
    private final MaterialRepository materialRepository;
    private final TipoBolsaRepository tipoBolsaRepository;
    private final UsuarioRepository usuarioRepository;
    private final Base64ValidatorUtil base64Validator;

    public PlantillaService(PlantillaRepository plantillaRepository,
                            MaterialRepository materialRepository,
                            TipoBolsaRepository tipoBolsaRepository,
                            UsuarioRepository usuarioRepository,
                            Base64ValidatorUtil base64Validator) {
        this.plantillaRepository = plantillaRepository;
        this.materialRepository = materialRepository;
        this.tipoBolsaRepository = tipoBolsaRepository;
        this.usuarioRepository = usuarioRepository;
        this.base64Validator = base64Validator;
    }

    /**
     * Obtiene todas las plantillas (sin base64 para optimizar)
     */
    public List<PlantillaSimpleDto> findAll() {
        log.debug("Obteniendo todas las plantillas");

        List<Plantilla> plantillas = plantillaRepository.findAll();

        log.info("Se encontraron {} plantillas", plantillas.size());

        return plantillas.stream()
                .map(this::mapToSimpleDto)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene una plantilla completa por ID (con base64)
     */
    public PlantillaResponseDto findById(Integer id) {
        log.debug("Obteniendo plantilla con ID: {}", id);

        Plantilla plantilla = plantillaRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Plantilla no encontrada: {}", id);
                    return new EntityNotFoundException("Plantilla", id);
                });

        log.debug("Plantilla encontrada: {}", plantilla.getNombre());
        return mapToResponseDto(plantilla);
    }

    /**
     * Crea una nueva plantilla
     */
    @Transactional
    public PlantillaResponseDto save(PlantillaCreateDto dto) {
        log.info("Iniciando creación de plantilla: {}", dto.getNombre());

        // 1. Validar que exista el material
        Material material = materialRepository.findById(dto.getMaterialId())
                .orElseThrow(() -> {
                    log.error("Material no encontrado: {}", dto.getMaterialId());
                    return new EntityNotFoundException("Material", dto.getMaterialId());
                });

        // 2. Validar que exista el tipo de bolsa
        TipoBolsa tipoBolsa = tipoBolsaRepository.findById(dto.getTipoBolsaId())
                .orElseThrow(() -> {
                    log.error("Tipo de bolsa no encontrado: {}", dto.getTipoBolsaId());
                    return new EntityNotFoundException("Tipo de Bolsa", dto.getTipoBolsaId());
                });

        // 3. Validar el base64 usando la utilidad
        base64Validator.validateBase64ForPlantillaOrDiseno(
                dto.getBase64Plantilla(),
                dto.getNombre()
        );

        // 4. Crear entidad
        Plantilla plantilla = new Plantilla();
        plantilla.setNombre(dto.getNombre());
        plantilla.setMaterial(material);
        plantilla.setTipoBolsa(tipoBolsa);
        plantilla.setBase64Plantilla(dto.getBase64Plantilla());
        plantilla.setAncho(dto.getAncho());
        plantilla.setAlto(dto.getAlto());
        plantilla.setProfundidad(dto.getProfundidad());

        // 5. Guardar
        Plantilla savedPlantilla = plantillaRepository.save(plantilla);

        log.info("Plantilla creada exitosamente con ID: {}", savedPlantilla.getId());

        return mapToResponseDto(savedPlantilla);
    }

    /**
     * Actualiza una plantilla existente
     */
    @Transactional
    public PlantillaResponseDto update(Integer id, PlantillaUpdateDto dto) {
        log.info("Iniciando actualización de plantilla con ID: {}", id);

        // 1. Buscar la plantilla
        Plantilla plantilla = plantillaRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Plantilla no encontrada: {}", id);
                    return new EntityNotFoundException("Plantilla", id);
                });

        // 2. Validar que exista el material
        Material material = materialRepository.findById(dto.getMaterialId())
                .orElseThrow(() -> {
                    log.error("Material no encontrado: {}", dto.getMaterialId());
                    return new EntityNotFoundException("Material", dto.getMaterialId());
                });

        // 3. Validar que exista el tipo de bolsa
        TipoBolsa tipoBolsa = tipoBolsaRepository.findById(dto.getTipoBolsaId())
                .orElseThrow(() -> {
                    log.error("Tipo de bolsa no encontrado: {}", dto.getTipoBolsaId());
                    return new EntityNotFoundException("Tipo de Bolsa", dto.getTipoBolsaId());
                });

        // 4. Actualizar datos básicos
        plantilla.setNombre(dto.getNombre());
        plantilla.setMaterial(material);
        plantilla.setTipoBolsa(tipoBolsa);
        plantilla.setAncho(dto.getAncho());
        plantilla.setAlto(dto.getAlto());
        plantilla.setProfundidad(dto.getProfundidad());

        // 5. Si viene nueva imagen, actualizar usando la utilidad
        if (dto.getBase64Plantilla() != null && !dto.getBase64Plantilla().isEmpty()) {
            base64Validator.validateBase64ForPlantillaOrDiseno(
                    dto.getBase64Plantilla(),
                    dto.getNombre()
            );
            plantilla.setBase64Plantilla(dto.getBase64Plantilla());
            log.debug("Imagen de la plantilla actualizada");
        }

        // 6. Guardar
        Plantilla updatedPlantilla = plantillaRepository.save(plantilla);

        log.info("Plantilla actualizada exitosamente: {}", id);

        return mapToResponseDto(updatedPlantilla);
    }

    /**
     * Elimina una plantilla
     */
    @Transactional
    public void deleteById(Integer id) {
        log.info("Iniciando eliminación de plantilla con ID: {}", id);

        if (!plantillaRepository.existsById(id)) {
            log.error("Plantilla no encontrada: {}", id);
            throw new EntityNotFoundException("Plantilla", id);
        }

        plantillaRepository.deleteById(id);

        log.info("Plantilla eliminada exitosamente: {}", id);
    }

    /**
     * Busca plantillas por material
     */
    public List<PlantillaSimpleDto> findByMaterial(Integer materialId) {
        log.debug("Buscando plantillas del material: {}", materialId);

        if (!materialRepository.existsById(materialId)) {
            log.error("Material no encontrado: {}", materialId);
            throw new EntityNotFoundException("Material", materialId);
        }

        List<Plantilla> plantillas = plantillaRepository.findByMaterialId(materialId);

        log.info("Se encontraron {} plantillas para el material: {}", plantillas.size(), materialId);

        return plantillas.stream()
                .map(this::mapToSimpleDto)
                .collect(Collectors.toList());
    }

    /**
     * Busca plantillas por tipo de bolsa
     */
    public List<PlantillaSimpleDto> findByTipoBolsa(Integer tipoBolsaId) {
        log.debug("Buscando plantillas del tipo de bolsa: {}", tipoBolsaId);

        if (!tipoBolsaRepository.existsById(tipoBolsaId)) {
            log.error("Tipo de bolsa no encontrado: {}", tipoBolsaId);
            throw new EntityNotFoundException("Tipo de Bolsa", tipoBolsaId);
        }

        List<Plantilla> plantillas = plantillaRepository.findByTipoBolsaId(tipoBolsaId);

        log.info("Se encontraron {} plantillas para el tipo de bolsa: {}", plantillas.size(), tipoBolsaId);

        return plantillas.stream()
                .map(this::mapToSimpleDto)
                .collect(Collectors.toList());
    }

    /**
     * Busca plantillas por nombre (búsqueda parcial)
     */
    public List<PlantillaSimpleDto> searchByNombre(String nombre) {
        log.debug("Buscando plantillas que contengan: {}", nombre);

        List<Plantilla> plantillas = plantillaRepository.findByNombreContainingIgnoreCase(nombre);

        log.info("Se encontraron {} plantillas con el criterio: {}", plantillas.size(), nombre);

        return plantillas.stream()
                .map(this::mapToSimpleDto)
                .collect(Collectors.toList());
    }

    /**
     * Habilita una plantilla para un usuario
     */
    @Transactional
    public void habilitarPlantillaParaUsuario(Integer plantillaId, UUID usuarioId) {
        log.info("Habilitando plantilla {} para usuario {}", plantillaId, usuarioId);

        Plantilla plantilla = plantillaRepository.findById(plantillaId)
                .orElseThrow(() -> {
                    log.error("Plantilla no encontrada: {}", plantillaId);
                    return new EntityNotFoundException("Plantilla", plantillaId);
                });

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> {
                    log.error("Usuario no encontrado: {}", usuarioId);
                    return new EntityNotFoundException("Usuario", usuarioId);
                });

        plantilla.getUsuariosHabilitados().add(usuario);
        plantillaRepository.save(plantilla);

        log.info("Plantilla {} habilitada para usuario {}", plantillaId, usuarioId);
    }

    /**
     * Deshabilita una plantilla para un usuario
     */
    @Transactional
    public void deshabilitarPlantillaParaUsuario(Integer plantillaId, UUID usuarioId) {
        log.info("Deshabilitando plantilla {} para usuario {}", plantillaId, usuarioId);

        Plantilla plantilla = plantillaRepository.findById(plantillaId)
                .orElseThrow(() -> {
                    log.error("Plantilla no encontrada: {}", plantillaId);
                    return new EntityNotFoundException("Plantilla", plantillaId);
                });

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> {
                    log.error("Usuario no encontrado: {}", usuarioId);
                    return new EntityNotFoundException("Usuario", usuarioId);
                });

        plantilla.getUsuariosHabilitados().remove(usuario);
        plantillaRepository.save(plantilla);

        log.info("Plantilla {} deshabilitada para usuario {}", plantillaId, usuarioId);
    }

    /**
     * Obtiene las plantillas habilitadas para un usuario
     */
    public List<PlantillaSimpleDto> findPlantillasHabilitadasParaUsuario(UUID usuarioId) {
        log.debug("Obteniendo plantillas habilitadas para usuario: {}", usuarioId);

        if (!usuarioRepository.existsById(usuarioId)) {
            log.error("Usuario no encontrado: {}", usuarioId);
            throw new EntityNotFoundException("Usuario", usuarioId);
        }

        List<Plantilla> plantillas = plantillaRepository.findByUsuariosHabilitadosId(usuarioId);

        log.info("Se encontraron {} plantillas habilitadas para el usuario: {}", plantillas.size(), usuarioId);

        return plantillas.stream()
                .map(this::mapToSimpleDto)
                .collect(Collectors.toList());
    }

    // ==================== MÉTODOS PRIVADOS ====================

    /**
     * Mapea entidad a DTO de respuesta completo
     */
    private PlantillaResponseDto mapToResponseDto(Plantilla plantilla) {
        return PlantillaResponseDto.builder()
                .id(plantilla.getId())
                .nombre(plantilla.getNombre())
                .base64Plantilla(plantilla.getBase64Plantilla())
                .material(MaterialResponseDto.builder()
                        .id(plantilla.getMaterial().getId())
                        .nombre(plantilla.getMaterial().getNombre())
                        .build())
                .tipoBolsa(TipoBolsaResponseDto.builder()
                        .id(plantilla.getTipoBolsa().getId())
                        .nombre(plantilla.getTipoBolsa().getNombre())
                        .build())
                .ancho(plantilla.getAncho())
                .alto(plantilla.getAlto())
                .profundidad(plantilla.getProfundidad())
                .build();
    }

    /**
     * Mapea entidad a DTO simple (sin base64)
     */
    private PlantillaSimpleDto mapToSimpleDto(Plantilla plantilla) {
        return PlantillaSimpleDto.builder()
                .id(plantilla.getId())
                .nombre(plantilla.getNombre())
                .materialNombre(plantilla.getMaterial().getNombre())
                .tipoBolsaNombre(plantilla.getTipoBolsa().getNombre())
                .ancho(plantilla.getAncho())
                .alto(plantilla.getAlto())
                .profundidad(plantilla.getProfundidad())
                .build();
    }
}