package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.dto.PlantillaDto;
import org.example.entity.*;
import org.example.service.PlantillaService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/plantillas")
public class PlantillaController {

    private final PlantillaService service;

    @GetMapping
    public List<PlantillaDto> list(){
        return service.getAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public PlantillaDto get(@PathVariable Integer id){ return toDto(service.getById(id)); }

    @PostMapping
    public PlantillaDto create(@RequestBody PlantillaDto dto){ return toDto(service.create(toEntity(dto))); }

    @PutMapping("/{id}")
    public PlantillaDto update(@PathVariable Integer id, @RequestBody PlantillaDto dto){
        return toDto(service.update(id, toEntity(dto)));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id){ service.delete(id); }

    @GetMapping("/created-after/{isoDateTime}")
    public List<PlantillaDto> createdAfter(@PathVariable String isoDateTime){
        return service.getCreatedAfter(LocalDateTime.parse(isoDateTime))
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    private PlantillaDto toDto(Plantilla p){
        return new PlantillaDto(
                p.getId(), p.getNombre(),
                p.getMaterial()!=null ? p.getMaterial().getId() : null,
                p.getTipoBolsa()!=null ? p.getTipoBolsa().getId() : null,
                p.getDimension()!=null ? p.getDimension().getId() : null,
                p.getFechaCreacion()
        );
    }

    private Plantilla toEntity(PlantillaDto d){
        Plantilla p = new Plantilla();
        p.setId(d.getId());
        p.setNombre(d.getNombre());
        if(d.getMaterialId()!=null){ Material m = new Material(); m.setId(d.getMaterialId()); p.setMaterial(m); }
        if(d.getTipoBolsaId()!=null){ TipoBolsa t = new TipoBolsa(); t.setId(d.getTipoBolsaId()); p.setTipoBolsa(t); }
        if(d.getDimensionId()!=null){ Dimension dim = new Dimension(); dim.setId(d.getDimensionId()); p.setDimension(dim); }
        p.setFechaCreacion(d.getFechaCreacion());
        return p;
    }
}
