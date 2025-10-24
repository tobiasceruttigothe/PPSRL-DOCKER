package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.dto.DisenoDto;
import org.example.entity.Diseno;
import org.example.entity.Imagen;
import org.example.entity.Plantilla;
import org.example.service.DisenoService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/disenos")
public class DisenoController {

    private final DisenoService service;

    @GetMapping
    public List<DisenoDto> list() {
        return service.list().stream().map(this::toDto).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public DisenoDto get(@PathVariable Integer id) {
        return toDto(service.get(id));
    }

    @PostMapping
    public DisenoDto create(@RequestBody DisenoDto dto) {
        return toDto(service.create(toEntity(dto)));
    }

    @PutMapping("/{id}")
    public DisenoDto update(@PathVariable Integer id, @RequestBody DisenoDto dto) {
        return toDto(service.update(id, toEntity(dto)));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }

    @GetMapping("/usuario/{usuarioId}")
    public List<DisenoDto> byUsuario(@PathVariable UUID usuarioId) {
        return service.findByUsuario(usuarioId).stream().map(this::toDto).collect(Collectors.toList());
    }

    @GetMapping("/plantilla/{plantillaId}")
    public List<DisenoDto> byPlantilla(@PathVariable Integer plantillaId) {
        return service.findByPlantilla(plantillaId).stream().map(this::toDto).collect(Collectors.toList());
    }

    // ===== mapping =====

    private DisenoDto toDto(Diseno d) {
        return new DisenoDto(
                d.getId(),
                d.getUsuarioId(),
                d.getPlantilla() != null ? d.getPlantilla().getId() : null,
                d.getImagen() != null ? d.getImagen().getId() : null,
                d.getNombre(),
                d.getDescripcion(),
                d.getStatus(),
                d.getFechaCreacion(),
                d.getFechaActualizacion()
        );
    }

    private Diseno toEntity(DisenoDto dto) {
        Diseno d = new Diseno();
        d.setId(dto.getId());
        d.setUsuarioId(dto.getUsuarioId());

        if (dto.getPlantillaId() != null) {
            Plantilla p = new Plantilla();
            p.setId(dto.getPlantillaId());
            d.setPlantilla(p);
        }

        if (dto.getImagenId() != null) {
            Imagen i = new Imagen();
            i.setId(dto.getImagenId());
            d.setImagen(i);
        }

        d.setNombre(dto.getNombre());
        d.setDescripcion(dto.getDescripcion());
        if (dto.getStatus() != null) {
            d.setStatus(dto.getStatus());
        }

        d.setFechaCreacion(dto.getFechaCreacion());
        d.setFechaActualizacion(dto.getFechaActualizacion());
        return d;
    }
}
