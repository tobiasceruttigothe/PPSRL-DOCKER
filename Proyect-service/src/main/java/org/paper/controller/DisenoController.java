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
import org.paper.dtoCreate.DisenoCreateDto;
import org.paper.dtoCreate.DisenoUpdateDto;
import org.paper.dtoResponse.DisenoResponseDto;
import org.paper.dtoResponse.DisenoSimpleDto;
import org.paper.entity.DisenoStatus;
import org.paper.service.DisenoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/disenos")
@Tag(name = "Diseños", description = "Gestión de diseños personalizados de bolsas creados por usuarios")
public class DisenoController {

    private final DisenoService disenoService;

    public DisenoController(DisenoService disenoService) {
        this.disenoService = disenoService;
    }

    @GetMapping
    @Operation(
            summary = "Obtener todos los diseños",
            description = """
            Retorna el listado completo de diseños (sin base64 para optimizar el tamaño de la respuesta).
            
            **Nota:** Para obtener la imagen completa, usar el endpoint GET /api/disenos/{id}
            """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de diseños obtenida exitosamente"
            )
    })
    public ResponseEntity<SuccessResponse<List<DisenoSimpleDto>>> obtenerDisenos() {
        log.info("Request: Obtener todos los diseños");

        List<DisenoSimpleDto> disenos = disenoService.findAll();

        return ResponseEntity.ok(
                SuccessResponse.of(
                        String.format("Se encontraron %d diseños", disenos.size()),
                        disenos
                )
        );
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Obtener un diseño por ID",
            description = "Retorna la información completa de un diseño específico, incluyendo la imagen en base64"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Diseño encontrado"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Diseño no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<SuccessResponse<DisenoResponseDto>> obtenerDisenoPorId(
            @Parameter(description = "ID del diseño", required = true, example = "1")
            @PathVariable Integer id) {

        log.info("Request: Obtener diseño con ID {}", id);

        DisenoResponseDto diseno = disenoService.findById(id);

        return ResponseEntity.ok(SuccessResponse.of(diseno));
    }

    @PostMapping
    @Operation(
            summary = "Crear un nuevo diseño",
            description = """
            Crea un nuevo diseño personalizado basado en una plantilla.
            
            **Validaciones:**
            - El usuario debe existir
            - La plantilla debe existir
            - El base64 debe ser válido
            - El tamaño máximo es 10MB
            - El estado inicial será PROGRESO
            """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Diseño creado exitosamente"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Usuario o Plantilla no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "Error procesando el archivo base64",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<SuccessResponse<DisenoResponseDto>> crearDiseno(
            @Valid @RequestBody DisenoCreateDto disenoCreateDto) {

        log.info("Request: Crear diseño '{}'", disenoCreateDto.getNombre());

        DisenoResponseDto diseno = disenoService.save(disenoCreateDto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(SuccessResponse.of("Diseño creado exitosamente", diseno));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Actualizar un diseño",
            description = """
            Actualiza los datos de un diseño existente.
            
            **Restricción:** No se puede actualizar un diseño en estado TERMINADO.
            
            **Nota:** El campo `base64Diseno` es opcional. Si no se envía, solo se actualizan los otros campos.
            """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Diseño actualizado exitosamente"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Diseño no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "No se puede actualizar un diseño en estado TERMINADO",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<SuccessResponse<DisenoResponseDto>> actualizarDiseno(
            @Parameter(description = "ID del diseño", required = true)
            @PathVariable Integer id,
            @Valid @RequestBody DisenoUpdateDto disenoUpdateDto) {

        log.info("Request: Actualizar diseño con ID {}", id);

        DisenoResponseDto diseno = disenoService.update(id, disenoUpdateDto);

        return ResponseEntity.ok(
                SuccessResponse.of("Diseño actualizado exitosamente", diseno)
        );
    }

    @PatchMapping("/{id}/terminar")
    @Operation(
            summary = "Marcar diseño como terminado",
            description = """
            Cambia el estado del diseño a TERMINADO.
            
            **Efecto:** Un diseño terminado no puede ser editado (solo puede reabrirse).
            """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Diseño marcado como terminado exitosamente"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Diseño no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "El diseño ya está en estado TERMINADO",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<SuccessResponse<DisenoResponseDto>> marcarComoTerminado(
            @Parameter(description = "ID del diseño", required = true)
            @PathVariable Integer id) {

        log.info("Request: Marcar diseño {} como TERMINADO", id);

        DisenoResponseDto diseno = disenoService.marcarComoTerminado(id);

        return ResponseEntity.ok(
                SuccessResponse.of("Diseño marcado como terminado", diseno)
        );
    }

    @PatchMapping("/{id}/reabrir")
    @Operation(
            summary = "Reabrir un diseño terminado",
            description = """
            Cambia el estado del diseño de TERMINADO a PROGRESO.
            
            **Efecto:** El diseño podrá ser editado nuevamente.
            """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Diseño reabierto exitosamente"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Diseño no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "El diseño ya está en estado PROGRESO",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<SuccessResponse<DisenoResponseDto>> marcarComoEnProgreso(
            @Parameter(description = "ID del diseño", required = true)
            @PathVariable Integer id) {

        log.info("Request: Marcar diseño {} como EN PROGRESO", id);

        DisenoResponseDto diseno = disenoService.marcarComoEnProgreso(id);

        return ResponseEntity.ok(
                SuccessResponse.of("Diseño reabierto", diseno)
        );
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Eliminar un diseño",
            description = """
            Elimina un diseño de forma permanente.
            
            ⚠️ **Esta acción es irreversible.**
            """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Diseño eliminado exitosamente"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Diseño no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<SuccessResponse<Void>> eliminarDiseno(
            @Parameter(description = "ID del diseño", required = true)
            @PathVariable Integer id) {

        log.info("Request: Eliminar diseño con ID {}", id);

        disenoService.deleteById(id);

        return ResponseEntity.ok(
                SuccessResponse.of("Diseño eliminado exitosamente")
        );
    }

    @DeleteMapping("/usuario/{usuarioId}/diseno/{disenoId}")
    @Operation(
            summary = "Eliminar un diseño específico de un usuario",
            description = """
            Elimina un diseño validando que pertenezca al usuario especificado.
            
            **Seguridad:** Solo se puede eliminar si el diseño pertenece al usuario.
            """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Diseño eliminado exitosamente"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "El diseño no pertenece al usuario",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Diseño no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<SuccessResponse<Void>> eliminarDisenoPorUsuario(
            @Parameter(description = "ID del usuario", required = true)
            @PathVariable UUID usuarioId,
            @Parameter(description = "ID del diseño", required = true)
            @PathVariable Integer disenoId) {

        log.info("Request: Eliminar diseño {} del usuario {}", disenoId, usuarioId);

        disenoService.deleteByUsuario(usuarioId, disenoId);

        return ResponseEntity.ok(
                SuccessResponse.of("Diseño eliminado exitosamente")
        );
    }

    @GetMapping("/usuario/{usuarioId}")
    @Operation(
            summary = "Obtener diseños de un usuario",
            description = """
        Retorna todos los diseños de un usuario específico incluyendo las imágenes en base64.
        
        **Nota:** Este endpoint incluye las imágenes para permitir vista previa en el frontend.
        """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de diseños obtenida exitosamente"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Usuario no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<SuccessResponse<List<DisenoResponseDto>>> obtenerDisenosPorUsuario(
            @Parameter(
                    description = "ID del usuario (UUID)",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID usuarioId) {

        log.info("Request: Obtener diseños del usuario {}", usuarioId);

        List<DisenoResponseDto> disenos = disenoService.findByUsuario(usuarioId);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        String.format("Se encontraron %d diseños", disenos.size()),
                        disenos
                )
        );
    }

    @GetMapping("/usuario/{usuarioId}/status/{status}")
    @Operation(
            summary = "Obtener diseños de un usuario por estado",
            description = """
        Retorna los diseños de un usuario filtrados por estado, incluyendo las imágenes en base64.
        
        **Estados disponibles:** PROGRESO, TERMINADO
        
        **Nota:** Este endpoint incluye las imágenes para permitir vista previa en el frontend.
        """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de diseños obtenida exitosamente"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Estado inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Usuario no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<SuccessResponse<List<DisenoResponseDto>>> obtenerDisenosPorUsuarioYEstado(
            @Parameter(description = "ID del usuario", required = true)
            @PathVariable UUID usuarioId,
            @Parameter(
                    description = "Estado del diseño",
                    required = true,
                    example = "PROGRESO"
            )
            @PathVariable String status) {

        log.info("Request: Obtener diseños del usuario {} en estado {}", usuarioId, status);

        DisenoStatus disenoStatus;
        try {
            disenoStatus = DisenoStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.error("Estado inválido: {}", status);
            throw new IllegalArgumentException("Estado inválido. Debe ser PROGRESO o TERMINADO");
        }

        List<DisenoResponseDto> disenos = disenoService.findByUsuarioAndStatus(usuarioId, disenoStatus);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        String.format("Se encontraron %d diseños en estado %s", disenos.size(), status),
                        disenos
                )
        );
    }

    @GetMapping("/plantilla/{plantillaId}")
    @Operation(
            summary = "Obtener diseños por plantilla",
            description = "Retorna todos los diseños creados con una plantilla específica"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de diseños obtenida exitosamente"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Plantilla no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<SuccessResponse<List<DisenoSimpleDto>>> obtenerDisenosPorPlantilla(
            @Parameter(description = "ID de la plantilla", required = true, example = "1")
            @PathVariable Integer plantillaId) {

        log.info("Request: Obtener diseños de la plantilla {}", plantillaId);

        List<DisenoSimpleDto> disenos = disenoService.findByPlantilla(plantillaId);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        String.format("Se encontraron %d diseños", disenos.size()),
                        disenos
                )
        );
    }

    @GetMapping("/search")
    @Operation(
            summary = "Buscar diseños por nombre",
            description = "Busca diseños cuyo nombre contenga el criterio especificado (case-insensitive)"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Búsqueda realizada exitosamente"
            )
    })
    public ResponseEntity<SuccessResponse<List<DisenoSimpleDto>>> buscarDisenos(
            @Parameter(
                    description = "Criterio de búsqueda (búsqueda parcial)",
                    required = true,
                    example = "bolsa"
            )
            @RequestParam String nombre) {

        log.info("Request: Buscar diseños con nombre que contenga '{}'", nombre);

        List<DisenoSimpleDto> disenos = disenoService.searchByNombre(nombre);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        String.format("Se encontraron %d diseños", disenos.size()),
                        disenos
                )
        );
    }

    @GetMapping("/usuario/{usuarioId}/count")
    @Operation(
            summary = "Contar diseños de un usuario",
            description = "Retorna la cantidad total de diseños que tiene un usuario"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Cantidad de diseños obtenida exitosamente"
            )
    })
    public ResponseEntity<SuccessResponse<Long>> contarDisenosPorUsuario(
            @Parameter(description = "ID del usuario", required = true)
            @PathVariable UUID usuarioId) {

        log.info("Request: Contar diseños del usuario {}", usuarioId);

        long count = disenoService.contarDisenosPorUsuario(usuarioId);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        String.format("El usuario tiene %d diseños", count),
                        count
                )
        );
    }

    @GetMapping("/usuario/{usuarioId}/count/{status}")
    @Operation(
            summary = "Contar diseños de un usuario por estado",
            description = "Retorna la cantidad de diseños de un usuario en un estado específico"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Cantidad de diseños obtenida exitosamente"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Estado inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<SuccessResponse<Long>> contarDisenosPorUsuarioYEstado(
            @Parameter(description = "ID del usuario", required = true)
            @PathVariable UUID usuarioId,
            @Parameter(
                    description = "Estado del diseño",
                    required = true,
                    example = "PROGRESO"
            )
            @PathVariable String status) {

        log.info("Request: Contar diseños del usuario {} en estado {}", usuarioId, status);

        DisenoStatus disenoStatus;
        try {
            disenoStatus = DisenoStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.error("Estado inválido: {}", status);
            throw new IllegalArgumentException("Estado inválido. Debe ser PROGRESO o TERMINADO");
        }

        long count = disenoService.contarDisenosPorUsuarioYEstado(usuarioId, disenoStatus);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        String.format("El usuario tiene %d diseños en estado %s", count, status),
                        count
                )
        );
    }

    //crear dos endpoints para modificar el nombre y la descripcion de un diseno por separado

    @PatchMapping("/{id}/nombre")
    @Operation(
            summary = "Actualizar el nombre de un diseño",
            description = "Actualiza únicamente el nombre de un diseño existente."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Nombre actualizado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Diseño no encontrado", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<SuccessResponse<DisenoResponseDto>> actualizarNombre(
            @Parameter(description = "ID del diseño", required = true)
            @PathVariable Integer id,
            @Parameter(description = "Nuevo nombre", required = true, example = "Nuevo nombre de diseño")
            @RequestBody String nuevoNombre) {

        log.info("Request: Actualizar nombre del diseño {} a '{}'", id, nuevoNombre);

        // Se asume que DisenoService tiene un método updateNombre(Integer id, String nuevoNombre)
        DisenoResponseDto diseno = disenoService.updateNombre(id, nuevoNombre);

        return ResponseEntity.ok(SuccessResponse.of("Nombre actualizado exitosamente", diseno));
    }

    @PatchMapping("/{id}/descripcion")
    @Operation(
            summary = "Actualizar la descripción de un diseño",
            description = "Actualiza únicamente la descripción de un diseño existente."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Descripción actualizada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Diseño no encontrado", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<SuccessResponse<DisenoResponseDto>> actualizarDescripcion(
            @Parameter(description = "ID del diseño", required = true)
            @PathVariable Integer id,
            @Parameter(description = "Nueva descripción", required = true, example = "Descripción actualizada")
            @RequestBody String nuevaDescripcion) {

        log.info("Request: Actualizar descripción del diseño {}", id);

        // Se asume que DisenoService tiene un método updateDescripcion(Integer id, String nuevaDescripcion)
        DisenoResponseDto diseno = disenoService.updateDescripcion(id, nuevaDescripcion);

        return ResponseEntity.ok(SuccessResponse.of("Descripción actualizada exitosamente", diseno));
    }




}

