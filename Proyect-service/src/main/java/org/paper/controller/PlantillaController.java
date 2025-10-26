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
import org.paper.dtoCreate.PlantillaCreateDto;
import org.paper.dtoCreate.PlantillaUpdateDto;
import org.paper.dtoResponse.PlantillaResponseDto;
import org.paper.dtoResponse.PlantillaSimpleDto;
import org.paper.service.PlantillaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/plantillas")
@Tag(name = "Plantillas", description = "Gestión de plantillas base para crear diseños de bolsas")
public class PlantillaController {

    private final PlantillaService plantillaService;

    public PlantillaController(PlantillaService plantillaService) {
        this.plantillaService = plantillaService;
    }

    @GetMapping
    @Operation(
            summary = "Obtener todas las plantillas",
            description = """
            Retorna el listado completo de plantillas (sin base64 para optimizar el tamaño de la respuesta).
            
            **Nota:** Para obtener la imagen completa, usar el endpoint GET /api/plantillas/{id}
            """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de plantillas obtenida exitosamente"
            )
    })
    public ResponseEntity<SuccessResponse<List<PlantillaSimpleDto>>> obtenerPlantillas() {
        log.info("Request: Obtener todas las plantillas");

        List<PlantillaSimpleDto> plantillas = plantillaService.findAll();

        return ResponseEntity.ok(
                SuccessResponse.of(
                        String.format("Se encontraron %d plantillas", plantillas.size()),
                        plantillas
                )
        );
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Obtener una plantilla por ID",
            description = "Retorna la información completa de una plantilla específica, incluyendo la imagen en base64"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Plantilla encontrada"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Plantilla no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<SuccessResponse<PlantillaResponseDto>> obtenerPlantillaPorId(
            @Parameter(description = "ID de la plantilla", required = true, example = "1")
            @PathVariable Integer id) {

        log.info("Request: Obtener plantilla con ID {}", id);

        PlantillaResponseDto plantilla = plantillaService.findById(id);

        return ResponseEntity.ok(SuccessResponse.of(plantilla));
    }

    @PostMapping
    @Operation(
            summary = "Crear una nueva plantilla",
            description = """
            Crea una nueva plantilla base para diseños de bolsas.
            
            **Validaciones:**
            - El material debe existir
            - El tipo de bolsa debe existir
            - El base64 debe ser válido
            - El tamaño máximo es 10MB
            - Dimensiones deben ser positivas
            """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Plantilla creada exitosamente"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Material o Tipo de Bolsa no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "Error procesando el archivo base64",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<SuccessResponse<PlantillaResponseDto>> crearPlantilla(
            @Valid @RequestBody PlantillaCreateDto plantillaCreateDto) {

        log.info("Request: Crear plantilla '{}'", plantillaCreateDto.getNombre());

        PlantillaResponseDto plantilla = plantillaService.save(plantillaCreateDto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(SuccessResponse.of("Plantilla creada exitosamente", plantilla));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Actualizar una plantilla",
            description = """
            Actualiza los datos de una plantilla existente.
            
            **Nota:** El campo `base64Plantilla` es opcional. Si no se envía, solo se actualizan los otros campos.
            """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Plantilla actualizada exitosamente"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Plantilla, Material o Tipo de Bolsa no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<SuccessResponse<PlantillaResponseDto>> actualizarPlantilla(
            @Parameter(description = "ID de la plantilla", required = true)
            @PathVariable Integer id,
            @Valid @RequestBody PlantillaUpdateDto plantillaUpdateDto) {

        log.info("Request: Actualizar plantilla con ID {}", id);

        PlantillaResponseDto plantilla = plantillaService.update(id, plantillaUpdateDto);

        return ResponseEntity.ok(
                SuccessResponse.of("Plantilla actualizada exitosamente", plantilla)
        );
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Eliminar una plantilla",
            description = """
            Elimina una plantilla de forma permanente.
            
            ⚠️ **Restricción:** No se puede eliminar si está siendo usada en diseños.
            ⚠️ **Esta acción es irreversible.**
            """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Plantilla eliminada exitosamente"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Plantilla no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "No se puede eliminar porque está siendo usada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<SuccessResponse<Void>> eliminarPlantilla(
            @Parameter(description = "ID de la plantilla", required = true)
            @PathVariable Integer id) {

        log.info("Request: Eliminar plantilla con ID {}", id);

        plantillaService.deleteById(id);

        return ResponseEntity.ok(
                SuccessResponse.of("Plantilla eliminada exitosamente")
        );
    }

    @GetMapping("/material/{materialId}")
    @Operation(
            summary = "Buscar plantillas por material",
            description = "Retorna todas las plantillas que usan un material específico"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Búsqueda realizada exitosamente"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Material no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<SuccessResponse<List<PlantillaSimpleDto>>> buscarPorMaterial(
            @Parameter(description = "ID del material", required = true, example = "1")
            @PathVariable Integer materialId) {

        log.info("Request: Buscar plantillas del material {}", materialId);

        List<PlantillaSimpleDto> plantillas = plantillaService.findByMaterial(materialId);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        String.format("Se encontraron %d plantillas", plantillas.size()),
                        plantillas
                )
        );
    }

    @GetMapping("/tipo-bolsa/{tipoBolsaId}")
    @Operation(
            summary = "Buscar plantillas por tipo de bolsa",
            description = "Retorna todas las plantillas de un tipo de bolsa específico"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Búsqueda realizada exitosamente"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Tipo de bolsa no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<SuccessResponse<List<PlantillaSimpleDto>>> buscarPorTipoBolsa(
            @Parameter(description = "ID del tipo de bolsa", required = true, example = "1")
            @PathVariable Integer tipoBolsaId) {

        log.info("Request: Buscar plantillas del tipo de bolsa {}", tipoBolsaId);

        List<PlantillaSimpleDto> plantillas = plantillaService.findByTipoBolsa(tipoBolsaId);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        String.format("Se encontraron %d plantillas", plantillas.size()),
                        plantillas
                )
        );
    }

    @GetMapping("/search")
    @Operation(
            summary = "Buscar plantillas por nombre",
            description = "Busca plantillas cuyo nombre contenga el criterio especificado (case-insensitive)"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Búsqueda realizada exitosamente"
            )
    })
    public ResponseEntity<SuccessResponse<List<PlantillaSimpleDto>>> buscarPlantillas(
            @Parameter(
                    description = "Criterio de búsqueda (búsqueda parcial)",
                    required = true,
                    example = "bolsa"
            )
            @RequestParam String nombre) {

        log.info("Request: Buscar plantillas con nombre que contenga '{}'", nombre);

        List<PlantillaSimpleDto> plantillas = plantillaService.searchByNombre(nombre);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        String.format("Se encontraron %d plantillas", plantillas.size()),
                        plantillas
                )
        );
    }

    @PostMapping("/{plantillaId}/habilitar-usuario/{usuarioId}")
    @Operation(
            summary = "Habilitar plantilla para un usuario",
            description = """
            Habilita una plantilla específica para que un usuario pueda usarla en sus diseños.
            
            **Nota:** Un usuario solo puede crear diseños con plantillas que tenga habilitadas.
            """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Plantilla habilitada exitosamente"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Plantilla o Usuario no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<SuccessResponse<Void>> habilitarPlantillaParaUsuario(
            @Parameter(description = "ID de la plantilla", required = true)
            @PathVariable Integer plantillaId,
            @Parameter(
                    description = "ID del usuario (UUID)",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID usuarioId) {

        log.info("Request: Habilitar plantilla {} para usuario {}", plantillaId, usuarioId);

        plantillaService.habilitarPlantillaParaUsuario(plantillaId, usuarioId);

        return ResponseEntity.ok(
                SuccessResponse.of("Plantilla habilitada exitosamente para el usuario")
        );
    }

    @DeleteMapping("/{plantillaId}/deshabilitar-usuario/{usuarioId}")
    @Operation(
            summary = "Deshabilitar plantilla para un usuario",
            description = """
            Deshabilita una plantilla para un usuario específico.
            
            **Nota:** El usuario no podrá crear nuevos diseños con esta plantilla.
            """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Plantilla deshabilitada exitosamente"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Plantilla o Usuario no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<SuccessResponse<Void>> deshabilitarPlantillaParaUsuario(
            @Parameter(description = "ID de la plantilla", required = true)
            @PathVariable Integer plantillaId,
            @Parameter(
                    description = "ID del usuario (UUID)",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID usuarioId) {

        log.info("Request: Deshabilitar plantilla {} para usuario {}", plantillaId, usuarioId);

        plantillaService.deshabilitarPlantillaParaUsuario(plantillaId, usuarioId);

        return ResponseEntity.ok(
                SuccessResponse.of("Plantilla deshabilitada exitosamente para el usuario")
        );
    }

    @GetMapping("/usuario/{usuarioId}/habilitadas")
    @Operation(
            summary = "Obtener plantillas habilitadas de un usuario",
            description = "Retorna todas las plantillas que un usuario tiene habilitadas para crear diseños"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de plantillas obtenida exitosamente"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Usuario no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<SuccessResponse<List<PlantillaSimpleDto>>> obtenerPlantillasHabilitadasDeUsuario(
            @Parameter(
                    description = "ID del usuario (UUID)",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID usuarioId) {

        log.info("Request: Obtener plantillas habilitadas para usuario {}", usuarioId);

        List<PlantillaSimpleDto> plantillas = plantillaService.findPlantillasHabilitadasParaUsuario(usuarioId);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        String.format("El usuario tiene %d plantillas habilitadas", plantillas.size()),
                        plantillas
                )
        );
    }
}