package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.dto.TipoBolsaDto;
import org.example.entity.TipoBolsa;
import org.example.service.TipoBolsaService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tipos-bolsa")
public class TipoBolsaController {

    private final TipoBolsaService service;

    @GetMapping
    public List<TipoBolsaDto> list() {
        return service.list().stream().map(this::toDto).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public TipoBolsaDto get(@PathVariable Integer id){ return toDto(service.get(id)); }

    @PostMapping
    public TipoBolsaDto create(@RequestBody TipoBolsaDto dto){ return toDto(service.create(toEntity(dto))); }

    @PutMapping("/{id}")
    public TipoBolsaDto update(@PathVariable Integer id, @RequestBody TipoBolsaDto dto){
        return toDto(service.update(id, toEntity(dto)));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id){ service.delete(id); }

    private TipoBolsaDto toDto(TipoBolsa t){ return new TipoBolsaDto(t.getId(), t.getNombre()); }
    private TipoBolsa toEntity(TipoBolsaDto d){ return new TipoBolsa(d.getId(), d.getNombre()); }
}
