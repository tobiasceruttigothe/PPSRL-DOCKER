package org.paper.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.paper.dto.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/ia")
@Tag(name = "IA - Generación de Imágenes", description = "Generación de vistas 3D usando Google Gemini")
public class ImageGenerationController {

    private final StabilityAIService stabilityAIService;

    public ImageGenerationController( StabilityAIService stabilityAIService) {
        this.stabilityAIService = stabilityAIService;
    }

    @PostMapping("/generate-3d")
    @Operation(
            summary = "Generar vista 3D de un diseño",
            description = """
            Genera una imagen 3D profesional y realista de una bolsa a partir de su diseño vectorizado.
            
            **Proceso:**
            1. Recibe el diseño vectorizado en base64
            2. Envía el diseño a Google Gemini con un prompt preestablecido
            3. Gemini genera una imagen 3D realista
            4. Retorna la imagen generada en base64
            
            **Nota:** La generación puede tardar entre 10-30 segundos.
            """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Imagen 3D generada exitosamente"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Error al generar la imagen (problema con Gemini API)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Servicio de Gemini no disponible",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<GenerateImageResponseDTO> generate3DViewstability(
            @Valid @RequestBody GenerateImageRequestDTO request) {

        log.info("Solicitud de generación 3D recibida para diseño ID: {}", request.getDisenoId());

        GenerateImageResponseDTO response = stabilityAIService.generate3DImage(request);

        log.info("Imagen 3D generada exitosamente para diseño ID: {}", request.getDisenoId());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    @Operation(
            summary = "Verificar estado del servicio",
            description = "Verifica si el servicio de IA está operativo y puede conectarse a Gemini API"
    )
    public ResponseEntity<String> healthCheckk() {
        boolean isHealthy = stabilityAIService.checkHealth();

        if (isHealthy) {
            return ResponseEntity.ok("Servicio de IA operativo - Conectado a Gemini API");
        } else {
            return ResponseEntity.status(503).body("Servicio de IA inoperativo - Error de conexión con Gemini API");
        }
    }

}