package org.paper.controllers;

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
import org.paper.dto.GenerateImageRequestDTO;
import org.paper.dto.GenerateImageResponseDTO;
import org.paper.services.ImagenGenerationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador para generación de vistas 3D usando Google Imagen 3
 */
@Slf4j
@RestController
@RequestMapping("/api/ia")
@Tag(name = "IA - Generación de Imágenes", description = "Generación de vistas 3D usando Google Imagen 3")
public class ImageGenerationController {

    private final ImagenGenerationService imagenService;

    public ImageGenerationController(ImagenGenerationService imagenService) {
        this.imagenService = imagenService;
    }

    @PostMapping("/generate-3d")
    @Operation(
            summary = "Generar vista 3D de un diseño",
            description = """
            Genera una vista 3D realista de una bolsa a partir de su diseño flat.
            
            **Proceso:**
            1. Recibe el ID del diseño
            2. Obtiene la imagen preview (base64_preview) de la BD
            3. Detecta el tipo de bolsa para aplicar el prompt correcto
            4. Envía la imagen a Google Imagen 3 con el prompt específico
            5. Guarda la imagen 3D generada en base64_vista3D
            
            **Tipos de bolsa soportados:**
            - Fondo Americano
            - Fondo Cuadrado con Manija
            - Fondo Cuadrado sin Manija
            - Genérico (para otros tipos)
            
            ⏱️ **Tiempo estimado:** 10-30 segundos
            """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Vista 3D generada exitosamente"
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
                    responseCode = "500",
                    description = "Error al generar la imagen con Google Imagen 3",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Servicio de Google Imagen 3 no disponible",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<GenerateImageResponseDTO> generate3DView(
            @Valid @RequestBody
            @Parameter(description = "ID del diseño a convertir a 3D", required = true)
            GenerateImageRequestDTO request) {

        log.info("📥 Solicitud de generación 3D recibida para diseño ID: {}", request.getDisenoId());

        GenerateImageResponseDTO response = imagenService.generate3DView(request.getDisenoId());

        if (response.isSuccess()) {
            log.info("✅ Imagen 3D generada exitosamente para diseño ID: {}", request.getDisenoId());
            return ResponseEntity.ok(response);
        } else {
            log.error("❌ Error generando imagen 3D: {}", response.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/health")
    @Operation(
            summary = "Verificar estado del servicio",
            description = """
            Verifica si el servicio de IA está operativo y puede conectarse a Google Vertex AI.
            
            **Checks realizados:**
            - Conexión con Google Cloud
            - Acceso a Vertex AI API
            - Validación de credenciales
            """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Servicio operativo"
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Servicio no disponible"
            )
    })
    public ResponseEntity<String> healthCheck() {
        log.info("🏥 Health check solicitado");

        boolean isHealthy = imagenService.checkHealth();

        if (isHealthy) {
            log.info("✅ Health check OK");
            return ResponseEntity.ok("✅ Servicio de IA operativo - Conectado a Google Vertex AI");
        } else {
            log.error("❌ Health check FAILED");
            return ResponseEntity.status(503)
                    .body("❌ Servicio de IA inoperativo - Error de conexión con Google Vertex AI");
        }
    }

    @GetMapping("/prompts")
    @Operation(
            summary = "Ver prompts configurados",
            description = "Retorna los prompts configurados para cada tipo de bolsa (útil para debugging)"
    )
    public ResponseEntity<String> getPrompts() {
        return ResponseEntity.ok("""
            📝 Prompts configurados:
            
            🎒 FONDO AMERICANO:
            - Genera render 3D realista con fondo americano
            - Mantiene colores y gráficos exactos
            - Muestra bolsa parada con iluminación natural
            
            🛍️ FONDO CUADRADO CON MANIJA:
            - Genera render 3D con base cuadrada y manijas
            - Mantiene diseño exacto del flat
            - Muestra manijas visibles y realistas
            
            📦 FONDO CUADRADO SIN MANIJA:
            - Genera render 3D con base cuadrada limpia
            - Sin manijas, borde superior limpio
            - Iluminación profesional
            
            🎨 GENÉRICO:
            - Infiere estructura desde diseño flat
            - Mantiene colores y gráficos exactos
            - Aplica mejor estimación de estructura
            """);
    }
}