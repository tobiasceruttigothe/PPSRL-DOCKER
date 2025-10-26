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
import org.paper.dtoCreate.MaterialCreateDto;
import org.paper.dtoCreate.MaterialUpdateDto;
import org.paper.dtoResponse.MaterialResponseDto;
import org.paper.service.MaterialService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/materiales")
@Tag(name = "Materiales", description = "Gestión del catálogo de materiales disponibles para bolsas")
public class MaterialController {

    private final MaterialService materialService;

    public MaterialController(MaterialService materialService) {
        this.materialService = materialService;
    }

    @GetMapping
    @Operation(
            summary = "Obtener todos los materiales",
            description = "Retorna el listado completo de materiales disponibles en el sistema"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de materiales obtenida exitosamente"
            )
    })
    public ResponseEntity<SuccessResponse<List<MaterialResponseDto>>> obtenerMateriales() {
        log.info("Request: Obtener todos los materiales");

        List<MaterialResponseDto> materiales = materialService.findAll();

        return ResponseEntity.ok(
                SuccessResponse.of(
                        String.format("Se encontraron %d materiales", materiales.size()),
                        materiales
                )
        );
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Obtener un material por ID",
            description = "Retorna la información de un material específico"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Material encontrado"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Material no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<SuccessResponse<MaterialResponseDto>> obtenerMaterialPorId(
            @Parameter(description = "ID del material", required = true, example = "1")
            @PathVariable Integer id) {

        log.info("Request: Obtener material con ID {}", id);

        MaterialResponseDto material = materialService.findById(id);

        return ResponseEntity.ok(SuccessResponse.of(material));
    }

    @PostMapping
    @Operation(
            summary = "Crear un nuevo material",
            description = """
            Crea un nuevo material en el catálogo.
            
            **Validaciones:**
            - El nombre debe ser único (case-insensitive)
            - El nombre debe tener entre 3 y 50 caracteres
            """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Material creado exitosamente"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Ya existe un material con ese nombre",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<SuccessResponse<MaterialResponseDto>> crearMaterial(
            @Valid @RequestBody MaterialCreateDto materialCreateDto) {

        log.info("Request: Crear material '{}'", materialCreateDto.getNombre());

        MaterialResponseDto material = materialService.save(materialCreateDto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(SuccessResponse.of("Material creado exitosamente", material));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Actualizar un material",
            description = "Actualiza el nombre de un material existente"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Material actualizado exitosamente"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Material no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Ya existe otro material con ese nombre",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<SuccessResponse<MaterialResponseDto>> actualizarMaterial(
            @Parameter(description = "ID del material", required = true)
            @PathVariable Integer id,
            @Valid @RequestBody MaterialUpdateDto materialUpdateDto) {

        log.info("Request: Actualizar material con ID {}", id);

        MaterialResponseDto material = materialService.update(id, materialUpdateDto);

        return ResponseEntity.ok(
                SuccessResponse.of("Material actualizado exitosamente", material)
        );
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Eliminar un material",
            description = """
            Elimina un material del catálogo.
            
            ⚠️ **Restricción:** No se puede eliminar si está siendo usado en plantillas.
            """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Material eliminado exitosamente"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Material no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "No se puede eliminar porque está siendo usado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<SuccessResponse<Void>> eliminarMaterial(
            @Parameter(description = "ID del material", required = true)
            @PathVariable Integer id) {

        log.info("Request: Eliminar material con ID {}", id);

        materialService.deleteById(id);

        return ResponseEntity.ok(
                SuccessResponse.of("Material eliminado exitosamente")
        );
    }

    @GetMapping("/search")
    @Operation(
            summary = "Buscar materiales por nombre",
            description = "Busca materiales cuyo nombre contenga el criterio especificado (case-insensitive)"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Búsqueda realizada exitosamente"
            )
    })
    public ResponseEntity<SuccessResponse<List<MaterialResponseDto>>> buscarMateriales(
            @Parameter(
                    description = "Criterio de búsqueda (búsqueda parcial)",
                    required = true,
                    example = "papel"
            )
            @RequestParam String nombre) {

        log.info("Request: Buscar materiales con nombre que contenga '{}'", nombre);

        List<MaterialResponseDto> materiales = materialService.searchByNombre(nombre);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        String.format("Se encontraron %d materiales", materiales.size()),
                        materiales
                )
        );
    }
}