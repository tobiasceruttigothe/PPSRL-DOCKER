package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.dto.UsuarioDto;
import org.example.entity.Usuario;
import org.example.service.UsuarioService;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService service;

    @GetMapping
    public List<UsuarioDto> list() {
        return service.getAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public UsuarioDto get(@PathVariable UUID id) {
        return toDto(service.getById(id));
    }

    @PostMapping
    public UsuarioDto create(@RequestBody UsuarioDto dto) {
        return toDto(service.create(toEntity(dto)));
    }

    @PutMapping("/{id}")
    public UsuarioDto update(@PathVariable UUID id, @RequestBody UsuarioDto dto) {
        return toDto(service.update(id, toEntity(dto)));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }

    @GetMapping("/status/{status}")
    public List<UsuarioDto> byStatus(@PathVariable String status) {
        return service.getByStatus(status)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/failed")
    public List<UsuarioDto> failed() {
        return service.getFailed()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/registered-after/{isoDateTime}")
    public List<UsuarioDto> registeredAfter(@PathVariable String isoDateTime) {
        return service.getRegisteredAfter(OffsetDateTime.parse(isoDateTime))
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // ======== Mapping ========

    private UsuarioDto toDto(Usuario u) {
        return new UsuarioDto(
                u.getId(),
                u.getFechaRegistro(),
                u.getStatus(),
                u.getIntentosActivacion(),
                u.getUltimoIntento(),
                u.getMotivoFallo()
        );
    }

    private Usuario toEntity(UsuarioDto dto) {
        Usuario u = new Usuario();
        u.setId(dto.getId());
        u.setFechaRegistro(dto.getFechaRegistro());
        u.setStatus(dto.getStatus());
        u.setIntentosActivacion(dto.getIntentosActivacion());
        u.setUltimoIntento(dto.getUltimoIntento());
        u.setMotivoFallo(dto.getMotivoFallo());
        return u;
    }
}
