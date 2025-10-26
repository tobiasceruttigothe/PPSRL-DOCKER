package org.paper.service;

import lombok.extern.slf4j.Slf4j;
import org.paper.dtoCreate.TipoBolsaCreateDto;
import org.paper.dtoCreate.TipoBolsaUpdateDto;
import org.paper.dtoResponse.TipoBolsaResponseDto;
import org.paper.entity.TipoBolsa;
import org.paper.exception.DuplicateEntityException;
import org.paper.exception.EntityNotFoundException;
import org.paper.repository.TipoBolsaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TipoBolsaService {

    private final TipoBolsaRepository tipoBolsaRepository;

    public TipoBolsaService(TipoBolsaRepository tipoBolsaRepository) {
        this.tipoBolsaRepository = tipoBolsaRepository;
    }

    /**
     * Obtiene todos los tipos de bolsa
     */
    public List<TipoBolsaResponseDto> findAll() {
        log.debug("Obteniendo todos los tipos de bolsa");

        List<TipoBolsa> tiposBolsa = tipoBolsaRepository.findAll();

        log.info("Se encontraron {} tipos de bolsa", tiposBolsa.size());

        return tiposBolsa.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene un tipo de bolsa por ID
     */
    public TipoBolsaResponseDto findById(Integer id) {
        log.debug("Obteniendo tipo de bolsa con ID: {}", id);

        TipoBolsa tipoBolsa = tipoBolsaRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Tipo de bolsa no encontrado: {}", id);
                    return new EntityNotFoundException("Tipo de Bolsa", id);
                });

        log.debug("Tipo de bolsa encontrado: {}", tipoBolsa.getNombre());
        return mapToResponseDto(tipoBolsa);
    }

    /**
     * Crea un nuevo tipo de bolsa
     */
    @Transactional
    public TipoBolsaResponseDto save(TipoBolsaCreateDto dto) {
        log.info("Iniciando creación de tipo de bolsa: {}", dto.getNombre());

        // Validar que no exista otro tipo de bolsa con el mismo nombre
        if (tipoBolsaRepository.existsByNombreIgnoreCase(dto.getNombre())) {
            log.error("Ya existe un tipo de bolsa con el nombre: {}", dto.getNombre());
            throw new DuplicateEntityException("Tipo de Bolsa", "nombre", dto.getNombre());
        }

        TipoBolsa tipoBolsa = new TipoBolsa();
        tipoBolsa.setNombre(dto.getNombre());

        TipoBolsa savedTipoBolsa = tipoBolsaRepository.save(tipoBolsa);

        log.info("Tipo de bolsa creado exitosamente con ID: {}", savedTipoBolsa.getId());

        return mapToResponseDto(savedTipoBolsa);
    }

    /**
     * Actualiza un tipo de bolsa existente
     */
    @Transactional
    public TipoBolsaResponseDto update(Integer id, TipoBolsaUpdateDto dto) {
        log.info("Iniciando actualización de tipo de bolsa con ID: {}", id);

        TipoBolsa tipoBolsa = tipoBolsaRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Tipo de bolsa no encontrado: {}", id);
                    return new EntityNotFoundException("Tipo de Bolsa", id);
                });

        // Validar que no exista otro tipo de bolsa con el mismo nombre (excepto el actual)
        if (tipoBolsaRepository.existsByNombreIgnoreCaseAndIdNot(dto.getNombre(), id)) {
            log.error("Ya existe otro tipo de bolsa con el nombre: {}", dto.getNombre());
            throw new DuplicateEntityException("Tipo de Bolsa", "nombre", dto.getNombre());
        }

        tipoBolsa.setNombre(dto.getNombre());

        TipoBolsa updatedTipoBolsa = tipoBolsaRepository.save(tipoBolsa);

        log.info("Tipo de bolsa actualizado exitosamente: {}", id);

        return mapToResponseDto(updatedTipoBolsa);
    }

    /**
     * Elimina un tipo de bolsa
     * NOTA: Solo se puede eliminar si no está siendo usado en plantillas
     */
    @Transactional
    public void deleteById(Integer id) {
        log.info("Iniciando eliminación de tipo de bolsa con ID: {}", id);

        if (!tipoBolsaRepository.existsById(id)) {
            log.error("Tipo de bolsa no encontrado: {}", id);
            throw new EntityNotFoundException("Tipo de Bolsa", id);
        }

        // La constraint RESTRICT en la BD impedirá la eliminación si está en uso
        // DataIntegrityViolationException será manejada por GlobalExceptionHandler
        tipoBolsaRepository.deleteById(id);

        log.info("Tipo de bolsa eliminado exitosamente: {}", id);
    }

    /**
     * Busca tipos de bolsa por nombre (búsqueda parcial)
     */
    public List<TipoBolsaResponseDto> searchByNombre(String nombre) {
        log.debug("Buscando tipos de bolsa que contengan: {}", nombre);

        List<TipoBolsa> tiposBolsa = tipoBolsaRepository.findByNombreContainingIgnoreCase(nombre);

        log.info("Se encontraron {} tipos de bolsa con el criterio: {}", tiposBolsa.size(), nombre);

        return tiposBolsa.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Mapea entidad a DTO de respuesta
     */
    private TipoBolsaResponseDto mapToResponseDto(TipoBolsa tipoBolsa) {
        return TipoBolsaResponseDto.builder()
                .id(tipoBolsa.getId())
                .nombre(tipoBolsa.getNombre())
                .build();
    }
}