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
import org.paper.services.GeminiImageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador para generación de vistas 3D usando Gemini 2.5 Flash Image
 * ACTUALIZADO: Ahora usa Gemini en lugar de Imagen 3
 */
@Slf4j
@RestController
@RequestMapping("/api/ia")
@Tag(name = "IA - Generación de Imágenes", description = "Generación de vistas 3D usando Gemini 2.5 Flash Image")
public class ImageGenerationController {

    private final GeminiImageService geminiImageService;

    public ImageGenerationController(GeminiImageService geminiImageService) {
        this.geminiImageService = geminiImageService;
    }

    @PostMapping("/generate-3d")
    @Operation(
            summary = "Generar vista 3D de un diseño",
            description = """
            Genera una vista 3D realista de una bolsa a partir de su diseño flat usando **Gemini 2.5 Flash Image**.
            
            **Proceso:**
            1. Recibe el ID del diseño
            2. Obtiene la imagen preview (base64_preview) de la BD
            3. Detecta el tipo de bolsa para aplicar el prompt correcto
            4. Envía la imagen + prompt a Gemini 2.5 Flash Image
            5. Gemini genera una vista 3D realista manteniendo el diseño exacto
            6. Guarda la imagen 3D generada en base64_preview (REEMPLAZA la anterior)
            
            **Tipos de bolsa soportados:**
            - Fondo Americano
            - Fondo Cuadrado con Manija
            - Fondo Cuadrado sin Manija
            - Genérico (para otros tipos)
            
            **Ventajas de Gemini 2.5 Flash Image:**
            - ✅ Acepta imagen de referencia + prompt (no solo texto)
            - ✅ Mantiene el diseño exacto del usuario
            - ✅ Genera vistas 3D fotorrealistas
            - ✅ Entendimiento contextual superior
            - ✅ Edición conversacional (multi-turn)
            
            ⏱️ **Tiempo estimado:** 5-15 segundos
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
                    description = "Error al generar la imagen con Gemini 2.5 Flash Image",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Servicio de Gemini 2.5 Flash Image no disponible",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<GenerateImageResponseDTO> generate3DView(
            @Valid @RequestBody
            @Parameter(description = "ID del diseño a convertir a 3D", required = true)
            GenerateImageRequestDTO request) {

        log.info("📥 Solicitud de generación 3D recibida para diseño ID: {}", request.getDisenoId());

        GenerateImageResponseDTO response = geminiImageService.generate3DView(request.getDisenoId());

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
            Verifica si el servicio de IA está operativo y puede conectarse a Gemini 2.5 Flash Image.
            
            **Checks realizados:**
            - Conexión con Google Cloud
            - Acceso a Vertex AI API
            - Validación de credenciales
            - Disponibilidad del modelo Gemini 2.5 Flash Image
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

        boolean isHealthy = geminiImageService.checkHealth();

        if (isHealthy) {
            log.info("✅ Health check OK");
            return ResponseEntity.ok("✅ Servicio de IA operativo - Conectado a Gemini 2.5 Flash Image");
        } else {
            log.error("❌ Health check FAILED");
            return ResponseEntity.status(503)
                    .body("❌ Servicio de IA inoperativo - Error de conexión con Gemini 2.5 Flash Image");
        }
    }

    @GetMapping("/model-info")
    @Operation(
            summary = "Información del modelo actual",
            description = "Retorna información sobre el modelo de IA utilizado"
    )
    public ResponseEntity<String> getModelInfo() {
        return ResponseEntity.ok("""
            🤖 **Modelo de IA Actual:** Gemini 2.5 Flash Image
            
            **Características:**
            - ✅ Generación de imágenes con imagen de referencia + prompt
            - ✅ Edición conversacional multi-turn
            - ✅ Mantiene consistencia de diseño
            - ✅ Fusión de múltiples imágenes
            - ✅ Entendimiento contextual profundo
            - ✅ SynthID watermark invisible incluido
            
            **Pricing:**
            - $0.039 por imagen generada (1290 tokens de salida)
            - Input sigue el precio de Gemini 2.5 Flash
            
            **Disponible en:** Vertex AI (us-central1)
            """);
    }

    @GetMapping("/prompts")
    @Operation(
            summary = "Ver prompts configurados",
            description = "Retorna los prompts configurados para cada tipo de bolsa (útil para debugging)"
    )
    public ResponseEntity<String> getPrompts() {
        return ResponseEntity.ok("""
            📝 **Prompts configurados para Gemini 2.5 Flash Image:**
            
            Todos los prompts siguen esta estructura:
            1. Reciben la imagen plana del diseño como referencia
            2. Solicitan transformación a vista 3D fotorrealista
            3. Mantienen el diseño exacto del usuario
            4. Aplican características específicas según tipo de bolsa
            
            🎒 **FONDO AMERICANO:**
            - Base rectangular con 4 esquinas plegadas
            - Textura kraft paper realista
            - Iluminación de estudio profesional
            
            🛍️ **FONDO CUADRADO CON MANIJA:**
            - Base cuadrada con pliegues limpios
            - Manijas de papel trenzado realistas
            - Sombras naturales bajo las manijas
            
            📦 **FONDO CUADRADO SIN MANIJA:**
            - Base cuadrada con borde superior limpio
            - Sin manijas, diseño minimalista
            - Iluminación profesional
            
            🎨 **GENÉRICO:**
            - Infiere estructura desde imagen de referencia
            - Mantiene diseño exacto
            - Vista 3D profesional adaptativa
            
            **IMPORTANTE:** El modelo SIEMPRE preserva el diseño exacto del usuario.
            """);
    }
}