package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.dto.UsuarioPlantillaDto;
import org.example.entity.Plantilla;
import org.example.entity.UsuarioPlantilla;
import org.example.entity.UsuarioPlantillaId;
import org.example.service.UsuarioPlantillaService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/usuarios-plantillas")
public class UsuarioPlantillaController {

    private final UsuarioPlantillaService service;

    @GetMapping
    public List<UsuarioPlantillaDto> list(){
        return service.list().stream().map(this::toDto).collect(Collectors.toList());
    }

    @GetMapping("/id")
    public UsuarioPlantillaDto get(@RequestParam UUID usuarioId, @RequestParam Integer plantillaId){
        return toDto(service.get(new UsuarioPlantillaId(usuarioId, plantillaId)));
    }

    @PostMapping
    public UsuarioPlantillaDto create(@RequestBody UsuarioPlantillaDto dto){
        return toDto(service.create(toEntity(dto)));
    }

    @DeleteMapping("/id")
    public void delete(@RequestParam UUID usuarioId, @RequestParam Integer plantillaId){
        service.delete(new UsuarioPlantillaId(usuarioId, plantillaId));
    }

    @GetMapping("/usuario/{usuarioId}")
    public List<UsuarioPlantillaDto> byUsuario(@PathVariable UUID usuarioId){
        return service.findByUsuario(usuarioId).stream().map(this::toDto).collect(Collectors.toList());
    }

    @GetMapping("/plantilla/{plantillaId}")
    public List<UsuarioPlantillaDto> byPlantilla(@PathVariable Integer plantillaId){
        return service.findByPlantilla(plantillaId).stream().map(this::toDto).collect(Collectors.toList());
    }

    private UsuarioPlantillaDto toDto(UsuarioPlantilla up){
        return new UsuarioPlantillaDto(
                up.getId()!=null ? up.getId().getUsuarioId() : null,
                up.getId()!=null ? up.getId().getPlantillaId() : null,
                up.getFechaHabilitacion()
        );
    }
    private UsuarioPlantilla toEntity(UsuarioPlantillaDto dto){
        UsuarioPlantilla up = new UsuarioPlantilla();
        up.setId(new UsuarioPlantillaId(dto.getUsuarioId(), dto.getPlantillaId()));
        // opcional: setear la relación plantilla por id si tu service la usa
        if(dto.getPlantillaId()!=null){
            Plantilla p = new Plantilla(); p.setId(dto.getPlantillaId()); up.setPlantilla(p);
        }
        up.setFechaHabilitacion(dto.getFechaHabilitacion());
        return up;
    }
}
