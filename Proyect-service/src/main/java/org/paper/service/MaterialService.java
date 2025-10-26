package org.paper.service;

import lombok.extern.slf4j.Slf4j;
import org.paper.dtoCreate.MaterialCreateDto;
import org.paper.dtoCreate.MaterialUpdateDto;
import org.paper.dtoResponse.MaterialResponseDto;
import org.paper.entity.Material;
import org.paper.exception.DuplicateEntityException;
import org.paper.exception.EntityNotFoundException;
import org.paper.repository.MaterialRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MaterialService {

    private final MaterialRepository materialRepository;

    public MaterialService(MaterialRepository materialRepository) {
        this.materialRepository = materialRepository;
    }

    /**
     * Obtiene todos los materiales
     */
    public List<MaterialResponseDto> findAll() {
        log.debug("Obteniendo todos los materiales");

        List<Material> materiales = materialRepository.findAll();

        log.info("Se encontraron {} materiales", materiales.size());

        return materiales.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene un material por ID
     */
    public MaterialResponseDto findById(Integer id) {
        log.debug("Obteniendo material con ID: {}", id);

        Material material = materialRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Material no encontrado: {}", id);
                    return new EntityNotFoundException("Material", id);
                });

        log.debug("Material encontrado: {}", material.getNombre());
        return mapToResponseDto(material);
    }

    /**
     * Crea un nuevo material
     */
    @Transactional
    public MaterialResponseDto save(MaterialCreateDto dto) {
        log.info("Iniciando creación de material: {}", dto.getNombre());

        // Validar que no exista otro material con el mismo nombre
        if (materialRepository.existsByNombreIgnoreCase(dto.getNombre())) {
            log.error("Ya existe un material con el nombre: {}", dto.getNombre());
            throw new DuplicateEntityException("Material", "nombre", dto.getNombre());
        }

        Material material = new Material();
        material.setNombre(dto.getNombre());

        Material savedMaterial = materialRepository.save(material);

        log.info("Material creado exitosamente con ID: {}", savedMaterial.getId());

        return mapToResponseDto(savedMaterial);
    }

    /**
     * Actualiza un material existente
     */
    @Transactional
    public MaterialResponseDto update(Integer id, MaterialUpdateDto dto) {
        log.info("Iniciando actualización de material con ID: {}", id);

        Material material = materialRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Material no encontrado: {}", id);
                    return new EntityNotFoundException("Material", id);
                });

        // Validar que no exista otro material con el mismo nombre (excepto el actual)
        if (materialRepository.existsByNombreIgnoreCaseAndIdNot(dto.getNombre(), id)) {
            log.error("Ya existe otro material con el nombre: {}", dto.getNombre());
            throw new DuplicateEntityException("Material", "nombre", dto.getNombre());
        }

        material.setNombre(dto.getNombre());

        Material updatedMaterial = materialRepository.save(material);

        log.info("Material actualizado exitosamente: {}", id);

        return mapToResponseDto(updatedMaterial);
    }

    /**
     * Elimina un material
     * NOTA: Solo se puede eliminar si no está siendo usado en plantillas
     */
    @Transactional
    public void deleteById(Integer id) {
        log.info("Iniciando eliminación de material con ID: {}", id);

        if (!materialRepository.existsById(id)) {
            log.error("Material no encontrado: {}", id);
            throw new EntityNotFoundException("Material", id);
        }

        // La constraint RESTRICT en la BD impedirá la eliminación si está en uso
        // DataIntegrityViolationException será manejada por GlobalExceptionHandler
        materialRepository.deleteById(id);

        log.info("Material eliminado exitosamente: {}", id);
    }

    /**
     * Busca materiales por nombre (búsqueda parcial)
     */
    public List<MaterialResponseDto> searchByNombre(String nombre) {
        log.debug("Buscando materiales que contengan: {}", nombre);

        List<Material> materiales = materialRepository.findByNombreContainingIgnoreCase(nombre);

        log.info("Se encontraron {} materiales con el criterio: {}", materiales.size(), nombre);

        return materiales.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Mapea entidad a DTO de respuesta
     */
    private MaterialResponseDto mapToResponseDto(Material material) {
        return MaterialResponseDto.builder()
                .id(material.getId())
                .nombre(material.getNombre())
                .build();
    }
}