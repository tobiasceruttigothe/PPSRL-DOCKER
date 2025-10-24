package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.dto.LogoDto;
import org.example.entity.Logo;
import org.example.service.LogoService;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/logos")
public class LogoController {

    private final LogoService service;

    @GetMapping
    public List<LogoDto> list() {
        return service.getAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public LogoDto get(@PathVariable Integer id) {
        return toDto(service.get(id));
    }

    @PostMapping
    public LogoDto create(@RequestBody LogoDto dto) {
        return toDto(service.create(toEntity(dto)));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }

    @GetMapping("/usuario/{usuarioId}")
    public List<LogoDto> byUsuario(@PathVariable UUID usuarioId) {
        return service.getByUsuario(usuarioId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private LogoDto toDto(Logo l) {
        return new LogoDto(
                l.getId(),
                l.getUsuarioId(),
                l.getNombre(),
                l.getTamanoBytes(),
                l.getFechaCreacion(),
                l.getLogo() != null ? Base64.getEncoder().encodeToString(l.getLogo()) : null
        );
    }

    private Logo toEntity(LogoDto d) {
        Logo l = new Logo();
        l.setId(d.getId());
        l.setUsuarioId(d.getUsuarioId());
        l.setNombre(d.getNombre());
        l.setTamanoBytes(d.getTamanoBytes());
        l.setFechaCreacion(d.getFechaCreacion());
        if (d.getBase64Logo() != null) {
            l.setLogo(Base64.getDecoder().decode(d.getBase64Logo()));
        }
        return l;
    }
}
