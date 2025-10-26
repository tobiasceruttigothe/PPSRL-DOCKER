package org.paper.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.paper.dto.ErrorResponse;
import org.paper.dto.SuccessResponse;
import org.paper.dtoCreate.TipoBolsaCreateDto;
import org.paper.dtoCreate.TipoBolsaUpdateDto;
import org.paper.dtoResponse.TipoBolsaResponseDto;
import org.paper.service.TipoBolsaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/tipos-bolsa")
@Tag(name = "Tipos de Bolsa", description = "Gestión del catálogo de tipos de bolsa disponibles")
public class TipoBolsaController {

    private final TipoBolsaService tipoBolsaService;

    public TipoBolsaController(TipoBolsaService tipoBolsaService) {
        this.tipoBolsaService = tipoBolsaService;
    }

    @GetMapping
    @Operation(
            summary = "Obtener todos los tipos de bolsa",
            description = "Retorna el listado completo de tipos de bolsa disponibles en el sistema"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de tipos de bolsa obtenida exitosamente"
            )
    })
    public ResponseEntity<SuccessResponse<List<TipoBolsaResponseDto>>> obtenerTiposBolsas() {
        log.info("Request: Obtener todos los tipos de bolsa");

        List<TipoBolsaResponseDto> tiposBolsa = tipoBolsaService.findAll();

        return ResponseEntity.ok(
                SuccessResponse.of(
                        String.format("Se encontraron %d tipos de bolsa", tiposBolsa.size()),
                        tiposBolsa
                )
        );
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Obtener un tipo de bolsa por ID",
            description = "Retorna la información de un tipo de bolsa específico"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Tipo de bolsa encontrado"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Tipo de bolsa no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<SuccessResponse<TipoBolsaResponseDto>> obtenerTipoBolsaPorId(
            @Parameter(description = "ID del tipo de bolsa", required = true, example = "1")
            @PathVariable Integer id) {

        log.info("Request: Obtener tipo de bolsa con ID {}", id);

        TipoBolsaResponseDto tipoBolsa = tipoBolsaService.findById(id);

        return ResponseEntity.ok(SuccessResponse.of(tipoBolsa));
    }

    @PostMapping
    @Operation(
            summary = "Crear un nuevo tipo de bolsa",
            description = """
            Crea un nuevo tipo de bolsa en el catálogo.
            
            **Validaciones:**
            - El nombre debe ser único (case-insensitive)
            - El nombre debe tener entre 3 y 50 caracteres
            """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Tipo de bolsa creado exitosamente"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Ya existe un tipo de bolsa con ese nombre",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<SuccessResponse<TipoBolsaResponseDto>> crearTipoBolsa(
            @Valid @RequestBody TipoBolsaCreateDto tipoBolsaCreateDto) {

        log.info("Request: Crear tipo de bolsa '{}'", tipoBolsaCreateDto.getNombre());

        TipoBolsaResponseDto tipoBolsa = tipoBolsaService.save(tipoBolsaCreateDto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(SuccessResponse.of("Tipo de bolsa creado exitosamente", tipoBolsa));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Actualizar un tipo de bolsa",
            description = "Actualiza el nombre de un tipo de bolsa existente"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Tipo de bolsa actualizado exitosamente"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Tipo de bolsa no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Ya existe otro tipo de bolsa con ese nombre",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<SuccessResponse<TipoBolsaResponseDto>> actualizarTipoBolsa(
            @Parameter(description = "ID del tipo de bolsa", required = true)
            @PathVariable Integer id,
            @Valid @RequestBody TipoBolsaUpdateDto tipoBolsaUpdateDto) {

        log.info("Request: Actualizar tipo de bolsa con ID {}", id);

        TipoBolsaResponseDto tipoBolsa = tipoBolsaService.update(id, tipoBolsaUpdateDto);

        return ResponseEntity.ok(
                SuccessResponse.of("Tipo de bolsa actualizado exitosamente", tipoBolsa)
        );
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Eliminar un tipo de bolsa",
            description = """
            Elimina un tipo de bolsa del catálogo.
            
            ⚠️ **Restricción:** No se puede eliminar si está siendo usado en plantillas.
            """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Tipo de bolsa eliminado exitosamente"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Tipo de bolsa no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "No se puede eliminar porque está siendo usado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<SuccessResponse<Void>> eliminarTipoBolsa(
            @Parameter(description = "ID del tipo de bolsa", required = true)
            @PathVariable Integer id) {

        log.info("Request: Eliminar tipo de bolsa con ID {}", id);

        tipoBolsaService.deleteById(id);

        return ResponseEntity.ok(
                SuccessResponse.of("Tipo de bolsa eliminado exitosamente")
        );
    }

    @GetMapping("/search")
    @Operation(
            summary = "Buscar tipos de bolsa por nombre",
            description = "Busca tipos de bolsa cuyo nombre contenga el criterio especificado (case-insensitive)"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Búsqueda realizada exitosamente"
            )
    })
    public ResponseEntity<SuccessResponse<List<TipoBolsaResponseDto>>> buscarTiposBolsa(
            @Parameter(
                    description = "Criterio de búsqueda (búsqueda parcial)",
                    required = true,
                    example = "asa"
            )
            @RequestParam String nombre) {

        log.info("Request: Buscar tipos de bolsa con nombre que contenga '{}'", nombre);

        List<TipoBolsaResponseDto> tiposBolsa = tipoBolsaService.searchByNombre(nombre);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        String.format("Se encontraron %d tipos de bolsa", tiposBolsa.size()),
                        tiposBolsa
                )
        );
    }
}