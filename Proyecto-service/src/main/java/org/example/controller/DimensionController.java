package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.dto.DimensionDto;
import org.example.entity.Dimension;
import org.example.service.DimensionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dimensiones")
public class DimensionController {

    private final DimensionService service;

    @GetMapping
    public List<DimensionDto> list(){
        return service.list().stream().map(this::toDto).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public DimensionDto get(@PathVariable Integer id){ return toDto(service.get(id)); }

    @PostMapping
    public DimensionDto create(@RequestBody DimensionDto dto){ return toDto(service.create(toEntity(dto))); }

    @PutMapping("/{id}")
    public DimensionDto update(@PathVariable Integer id, @RequestBody DimensionDto dto){
        return toDto(service.update(id, toEntity(dto)));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id){ service.delete(id); }

    private DimensionDto toDto(Dimension d){ return new DimensionDto(d.getId(), d.getAnchoMm(), d.getAltoMm()); }
    private Dimension toEntity(DimensionDto dto){
        Dimension d = new Dimension();
        d.setId(dto.getId()); d.setAnchoMm(dto.getAnchoMm()); d.setAltoMm(dto.getAltoMm());
        return d;
    }
}
