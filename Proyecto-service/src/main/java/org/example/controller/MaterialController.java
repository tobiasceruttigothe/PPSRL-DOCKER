package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.dto.MaterialDto;
import org.example.entity.Material;
import org.example.service.MaterialService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/materiales")
public class MaterialController {

    private final MaterialService service;

    @GetMapping
    public List<MaterialDto> list() {
        return service.list().stream().map(this::toDto).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public MaterialDto get(@PathVariable Integer id) { return toDto(service.get(id)); }

    @PostMapping
    public MaterialDto create(@RequestBody MaterialDto dto) {
        return toDto(service.create(toEntity(dto)));
    }

    @PutMapping("/{id}")
    public MaterialDto update(@PathVariable Integer id, @RequestBody MaterialDto dto) {
        return toDto(service.update(id, toEntity(dto)));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) { service.delete(id); }

    // ===== mapping =====
    private MaterialDto toDto(Material m){ return new MaterialDto(m.getId(), m.getNombre()); }
    private Material toEntity(MaterialDto d){ return new Material(d.getId(), d.getNombre()); }
}
