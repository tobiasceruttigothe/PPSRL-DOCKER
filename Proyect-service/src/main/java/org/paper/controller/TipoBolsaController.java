package org.paper.controller;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.paper.dtoCreate.MaterialCreateDto;
import org.paper.dtoCreate.TipoBolsaCreateDto;
import org.paper.service.MaterialService;
import org.paper.service.TipoBolsaService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;



@Slf4j
@RestController
@RequestMapping("/api/tipos-bolsa")
@Tag(name = "Tipos de bolsas", description = "Gestión de tipos de bolsas del sistema")
public class TipoBolsaController {

    private final TipoBolsaService tipoBolsaService;

    public TipoBolsaController(TipoBolsaService tipoBolsaService) {
        this.tipoBolsaService = tipoBolsaService;
    }

    @GetMapping
    public ResponseEntity<?> obtenerTiposBolsas() {
        return ResponseEntity.ok(tipoBolsaService.findAll());
    }

    @GetMapping("/{tipoBolsaId}")
    public ResponseEntity<?> obtenerTipoBolsaPorId(@PathVariable Integer tipoBolsaId) {
        return ResponseEntity.ok(tipoBolsaService.findById(tipoBolsaId));
    }

    @PostMapping()
    public ResponseEntity<?> crearTipoBolsa(@Valid TipoBolsaCreateDto tipoBolsaCreateDto) {
        return ResponseEntity.ok(tipoBolsaService.save(tipoBolsaCreateDto));
    }

    @DeleteMapping("/{materialId}")
    public ResponseEntity<?> eliminarMaterial(@PathVariable Integer materialId) {
        tipoBolsaService.deleteById(materialId);
        return ResponseEntity.ok().build();
    }
}


