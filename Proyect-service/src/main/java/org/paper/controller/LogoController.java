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
import org.paper.dtoCreate.LogoCreateDto;
import org.paper.dtoCreate.LogoUpdateDto;
import org.paper.dtoResponse.LogoResponseDto;
import org.paper.service.LogoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/logos")
@Tag(name = "Logos", description = "Gestión de logos personalizados de los usuarios")
public class LogoController {

    private final LogoService logoService;

    public LogoController(LogoService logoService) {
        this.logoService = logoService;
    }

    @PostMapping
    @Operation(
            summary = "Crear un nuevo logo",
            description = """
            Crea un nuevo logo para un usuario.
            
            **Validaciones:**
            - El usuario debe existir en la base de datos
            - El nombre debe tener entre 3 y 100 caracteres
            - El base64 debe ser válido
            - El tamaño máximo es 5MB
            
            **Formato del base64:**
            - Puede incluir el prefijo: `data:image/png;base64,`
            - O solo el string base64 puro
            """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Logo creado exitosamente",
                    content = @Content(schema = @Schema(implementation = LogoResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Usuario no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "Error procesando el archivo base64",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<SuccessResponse<LogoResponseDto>> crearLogo(
            @Valid @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Datos del logo a crear",
                    required = true
            )
            LogoCreateDto logoCreateDto) {

        log.info("Request: Crear logo '{}' para usuario {}",
                logoCreateDto.getNombre(), logoCreateDto.getUsuarioId());

        LogoResponseDto logo = logoService.crearLogo(logoCreateDto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(SuccessResponse.of("Logo creado exitosamente", logo));
    }

    @GetMapping("/usuario/{usuarioId}")
    @Operation(
            summary = "Obtener logos de un usuario",
            description = """
            Retorna todos los logos asociados a un usuario específico.
            
            **Nota:** Si el usuario no tiene logos, retorna una lista vacía.
            """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de logos obtenida exitosamente"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Usuario no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<SuccessResponse<List<LogoResponseDto>>> obtenerLogosPorUsuario(
            @Parameter(
                    description = "ID del usuario (UUID)",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID usuarioId) {

        log.info("Request: Obtener logos del usuario {}", usuarioId);

        List<LogoResponseDto> logos = logoService.obtenerLogosPorUsuario(usuarioId);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        String.format("Se encontraron %d logos", logos.size()),
                        logos
                )
        );
    }

    @GetMapping("/{logoId}")
    @Operation(
            summary = "Obtener un logo por ID",
            description = "Retorna la información completa de un logo específico"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Logo encontrado"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Logo no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<SuccessResponse<LogoResponseDto>> obtenerLogoPorId(
            @Parameter(
                    description = "ID del logo",
                    required = true,
                    example = "1"
            )
            @PathVariable Integer logoId) {

        log.info("Request: Obtener logo con ID {}", logoId);

        LogoResponseDto logo = logoService.obtenerLogoPorId(logoId);

        return ResponseEntity.ok(SuccessResponse.of(logo));
    }

    @PutMapping("/{logoId}")
    @Operation(
            summary = "Actualizar un logo",
            description = """
            Actualiza el nombre y/o la imagen de un logo existente.
            
            **Nota:** El campo `base64Logo` es opcional. Si no se envía, solo se actualiza el nombre.
            """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Logo actualizado exitosamente"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Logo no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<SuccessResponse<LogoResponseDto>> actualizarLogo(
            @Parameter(description = "ID del logo", required = true)
            @PathVariable Integer logoId,
            @Valid @RequestBody LogoUpdateDto logoUpdateDto) {

        log.info("Request: Actualizar logo con ID {}", logoId);

        LogoResponseDto logo = logoService.actualizarLogo(logoId, logoUpdateDto);

        return ResponseEntity.ok(
                SuccessResponse.of("Logo actualizado exitosamente", logo)
        );
    }

    @DeleteMapping("/{logoId}")
    @Operation(
            summary = "Eliminar un logo",
            description = """
            Elimina un logo de forma permanente.
            
            ⚠️ **Esta acción es irreversible.**
            """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Logo eliminado exitosamente"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Logo no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<SuccessResponse<Void>> eliminarLogo(
            @Parameter(description = "ID del logo", required = true)
            @PathVariable Integer logoId) {

        log.info("Request: Eliminar logo con ID {}", logoId);

        logoService.eliminarLogo(logoId);

        return ResponseEntity.ok(
                SuccessResponse.of("Logo eliminado exitosamente")
        );
    }

    @DeleteMapping("/usuario/{usuarioId}/logo/{logoId}")
    @Operation(
            summary = "Eliminar un logo específico de un usuario",
            description = """
            Elimina un logo validando que pertenezca al usuario especificado.
            
            **Seguridad:** Solo se puede eliminar si el logo pertenece al usuario.
            """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Logo eliminado exitosamente"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "El logo no pertenece al usuario",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Logo no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<SuccessResponse<Void>> eliminarLogoPorUsuario(
            @Parameter(description = "ID del usuario", required = true)
            @PathVariable UUID usuarioId,
            @Parameter(description = "ID del logo", required = true)
            @PathVariable Integer logoId) {

        log.info("Request: Eliminar logo {} del usuario {}", logoId, usuarioId);

        logoService.eliminarLogoPorUsuario(usuarioId, logoId);

        return ResponseEntity.ok(
                SuccessResponse.of("Logo eliminado exitosamente")
        );
    }

    @GetMapping("/usuario/{usuarioId}/count")
    @Operation(
            summary = "Contar logos de un usuario",
            description = "Retorna la cantidad total de logos que tiene un usuario"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Cantidad de logos obtenida exitosamente"
            )
    })
    public ResponseEntity<SuccessResponse<Long>> contarLogosPorUsuario(
            @Parameter(description = "ID del usuario", required = true)
            @PathVariable UUID usuarioId) {

        log.info("Request: Contar logos del usuario {}", usuarioId);

        long count = logoService.contarLogosPorUsuario(usuarioId);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        String.format("El usuario tiene %d logos", count),
                        count
                )
        );
    }
}