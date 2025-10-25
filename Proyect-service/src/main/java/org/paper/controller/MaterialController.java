package org.paper.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.paper.dtoCreate.MaterialCreateDto;
import org.paper.service.MaterialService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;



@Slf4j
@RestController
@RequestMapping("/api/materiales")
@Tag(name = "Materiales", description = "Gestión de Materiales del sistema")
public class MaterialController {

    private final MaterialService materialService;

    public MaterialController(MaterialService materialService) {
        this.materialService = materialService;
    }

    @GetMapping
    public ResponseEntity<?> obtenerMateriales() {
        return ResponseEntity.ok(materialService.findAll());
    }

    @PostMapping()
    public ResponseEntity<?> crearMaterial(@Valid MaterialCreateDto materialCreateDto) {
        return ResponseEntity.ok(materialService.save(materialCreateDto));
    }

    @DeleteMapping("/{materialId}")
    public ResponseEntity<?> eliminarMaterial(@PathVariable Integer materialId) {
        materialService.deleteById(materialId);
        return ResponseEntity.ok().build();
    }
}


