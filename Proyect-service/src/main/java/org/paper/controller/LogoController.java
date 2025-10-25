package org.paper.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.paper.dtoCreate.LogoCreateDto;
import org.paper.service.LogoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;


@Slf4j
@RestController
@RequestMapping("/api/logos")
@Tag(name = "Logos", description = "Gestión de logos de los usuarios")
public class LogoController {

    private final LogoService logoService;

    public LogoController(LogoService logoService) {
        this.logoService = logoService;
    }

    @GetMapping("/{usuarioId}")
    public ResponseEntity<?> obtenerLogoPorUsuarioId(@PathVariable UUID usuarioId) {
        return ResponseEntity.ok(logoService.obtenerLogoPorUsuarioId(usuarioId));
    }

    @PostMapping()
    public ResponseEntity<?> crearLogo(@Valid LogoCreateDto logoCreateDto) {

        return ResponseEntity.ok(logoService.crearLogo(logoCreateDto));
    }

    @DeleteMapping("/{usuarioId}/{idLogo}")
    public ResponseEntity<?> eliminarLogo(@PathVariable UUID usuarioId, Integer idLogo ) {
        return ResponseEntity.ok(logoService.eliminarLogoPorUsuarioId(usuarioId, idLogo));
    }
}


