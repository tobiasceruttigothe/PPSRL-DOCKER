package org.paper.services;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.Content;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.api.GenerationConfig;
import com.google.cloud.vertexai.api.Part;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.paper.dto.GenerateImageResponseDTO;
import org.paper.dto.TipoBolsaEnum;
import org.paper.entity.Diseno;
import org.paper.exception.ImageGenerationException;
import org.paper.repository.DisenoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Servicio para generar vistas 3D usando Gemini 2.5 Flash Image
 * Este servicio reemplaza a ImagenGenerationService
 */
@Slf4j
@Service
public class GeminiImageService {

    private final DisenoRepository disenoRepository;
    private final String projectId;
    private final String location;
    private final String modelName;

    private final VertexAI vertexAI;

    // Prompts específicos por tipo de bolsa
    private static final Map<TipoBolsaEnum, String> PROMPTS = new HashMap<>();

    static {
        PROMPTS.put(TipoBolsaEnum.FONDO_AMERICANO,
                """
                Transform this flat bag design into a professional 3D product mockup of a kraft paper shopping bag with American bottom style.
                
                Requirements:
                - Standing upright on a clean white surface with soft studio lighting
                - American bottom construction with rectangular base and 4 folded corners clearly visible
                - Natural brown kraft paper texture with realistic fiber details
                - Apply the EXACT design from the reference image onto the front of the bag, maintaining all colors, logos, and graphics
                - Realistic shadows cast on the surface beneath
                - Professional commercial photography quality
                - Photorealistic 3D visualization with studio lighting
                
                IMPORTANT: Keep the design from the reference image exactly as shown, just render it on a 3D bag.
                """
        );

        PROMPTS.put(TipoBolsaEnum.FONDO_CUADRADO_CON_MANIJA,
                """
                Transform this flat bag design into a professional 3D product mockup of a kraft paper shopping bag with square bottom and twisted paper handles.
                
                Requirements:
                - Standing upright on a clean white surface with soft studio lighting
                - Square flat bottom base with clean fold lines
                - Twisted kraft paper handles attached to the top, hanging naturally
                - Natural brown kraft paper texture with realistic fiber details
                - Apply the EXACT design from the reference image onto the front of the bag, maintaining all colors, logos, and graphics
                - Handles should show realistic paper twist texture and proper attachment points
                - Realistic shadows cast on the surface beneath and under the handles
                - Professional commercial photography quality
                - Photorealistic 3D visualization with studio lighting
                
                IMPORTANT: Keep the design from the reference image exactly as shown, just render it on a 3D bag with handles.
                """
        );

        PROMPTS.put(TipoBolsaEnum.FONDO_CUADRADO_SIN_MANIJA,
                """
                Transform this flat bag design into a professional 3D product mockup of a kraft paper shopping bag with square bottom and no handles.
                
                Requirements:
                - Standing upright on a clean white surface with soft studio lighting
                - Square flat bottom base with clean fold lines clearly visible
                - Clean top edge with no handles
                - Natural brown kraft paper texture with realistic fiber details
                - Apply the EXACT design from the reference image onto the front of the bag, maintaining all colors, logos, and graphics
                - Realistic shadows cast on the surface beneath
                - Professional commercial photography quality
                - Photorealistic 3D visualization with studio lighting
                
                IMPORTANT: Keep the design from the reference image exactly as shown, just render it on a 3D bag.
                """
        );

        PROMPTS.put(TipoBolsaEnum.GENERICO,
                """
                Transform this flat bag design into a professional 3D product mockup of a kraft paper shopping bag.
                
                Requirements:
                - Standing upright on a clean white surface with soft studio lighting
                - Natural brown kraft paper texture with realistic fiber details
                - Apply the EXACT design from the reference image onto the front of the bag, maintaining all colors, logos, and graphics
                - Realistic shadows cast on the surface beneath
                - Professional commercial photography quality
                - Photorealistic 3D visualization with studio lighting
                
                IMPORTANT: Keep the design from the reference image exactly as shown, just render it on a 3D bag.
                """
        );
    }

    public GeminiImageService(
            DisenoRepository disenoRepository,
            @Value("${google.cloud.project-id}") String projectId,
            @Value("${google.cloud.location}") String location,
            @Value("${gemini.model-name:gemini-2.5-flash-image}") String modelName) {
        this.disenoRepository = disenoRepository;
        this.projectId = projectId;
        this.location = location;
        this.modelName = modelName;

        // Inicializar VertexAI
        this.vertexAI = new VertexAI(projectId, location);

        log.info("✅ GeminiImageService inicializado - Proyecto: {}, Location: {}, Model: {}",
                projectId, location, modelName);
    }

    /**
     * Genera vista 3D para un diseño específico usando Gemini 2.5 Flash Image
     */
    @Transactional
    public GenerateImageResponseDTO generate3DView(Integer disenoId) {
        log.info("🎨 Iniciando generación de vista 3D con Gemini para diseño ID: {}", disenoId);

        try {
            // 1. Buscar el diseño
            Diseno diseno = disenoRepository.findById(disenoId)
                    .orElseThrow(() -> new ImageGenerationException(
                            "Diseño no encontrado con ID: " + disenoId));

            // 2. Validar que tenga imagen preview
            if (diseno.getBase64Preview() == null || diseno.getBase64Preview().isEmpty()) {
                throw new ImageGenerationException(
                        "El diseño no tiene imagen preview para generar vista 3D");
            }

            // 3. Obtener tipo de bolsa
            TipoBolsaEnum tipoBolsa = TipoBolsaEnum.fromNombre(
                    diseno.getPlantilla().getTipoBolsa().getNombre());

            log.info("📦 Tipo de bolsa detectado: {}", tipoBolsa.getDisplayName());

            // 4. Generar imagen 3D con Gemini
            String nuevaImagen3D = generateImageWithGemini(
                    diseno.getBase64Preview(),
                    tipoBolsa);

            // 5. Actualizar diseño con nueva imagen
            diseno.setBase64Preview(nuevaImagen3D);
            diseno.setFechaActualizacion(LocalDateTime.now());
            disenoRepository.save(diseno);

            log.info("✅ Vista 3D generada y guardada exitosamente para diseño ID: {}", disenoId);

            return GenerateImageResponseDTO.success(disenoId);

        } catch (ImageGenerationException e) {
            log.error("❌ Error generando vista 3D: {}", e.getMessage());
            return GenerateImageResponseDTO.error(disenoId,
                    "Error al generar vista 3D", e.getMessage());
        } catch (Exception e) {
            log.error("❌ Error inesperado generando vista 3D", e);
            return GenerateImageResponseDTO.error(disenoId,
                    "Error inesperado", e.getMessage());
        }
    }

    /**
     * Genera imagen 3D usando Gemini 2.5 Flash Image
     * Recibe: imagen base64 + prompt
     * Retorna: nueva imagen base64
     */
    private String generateImageWithGemini(String base64Preview, TipoBolsaEnum tipoBolsa) {
        try {
            log.info("📡 Llamando a Gemini 2.5 Flash Image API...");
            log.info("🎨 Tipo de bolsa: {}", tipoBolsa.getDisplayName());

            // Obtener prompt según tipo de bolsa
            String prompt = PROMPTS.get(tipoBolsa);
            log.info("📝 Prompt: {}", prompt.substring(0, Math.min(150, prompt.length())) + "...");

            // Limpiar base64 (remover prefijo data:image/...)
            String cleanBase64 = cleanBase64(base64Preview);
            byte[] imageBytes = Base64.getDecoder().decode(cleanBase64);

            // Crear el modelo generativo
            GenerativeModel model = new GenerativeModel(modelName, vertexAI)
                    .withGenerationConfig(
                            GenerationConfig.newBuilder()
                                    .setTemperature(0.9f)
                                    .setMaxOutputTokens(8192)
                                    .build()
                    );

            // Construir el contenido multimodal (imagen + texto)
            Content content = Content.newBuilder()
                    .setRole("user")
                    .addParts(
                            Part.newBuilder()
                                    .setInlineData(
                                            com.google.cloud.vertexai.api.Blob.newBuilder()
                                                    .setMimeType("image/png")
                                                    .setData(ByteString.copyFrom(imageBytes))
                                                    .build()
                                    )
                                    .build()
                    )
                    .addParts(
                            Part.newBuilder()
                                    .setText(prompt)
                                    .build()
                    )
                    .build();

            log.info("🚀 Enviando request a Gemini...");

            // Generar contenido
            GenerateContentResponse response = model.generateContent(content);

            log.info("📥 Respuesta recibida de Gemini");

            // Extraer imagen generada
            if (response.getCandidatesCount() > 0) {
                var candidate = response.getCandidates(0);

                for (var part : candidate.getContent().getPartsList()) {
                    if (part.hasInlineData()) {
                        ByteString imageData = part.getInlineData().getData();
                        String generatedBase64 = Base64.getEncoder().encodeToString(imageData.toByteArray());

                        log.info("✅ Imagen generada exitosamente por Gemini 2.5 Flash Image");
                        log.info("📊 Tamaño de imagen generada: {} bytes", imageData.size());

                        // Retornar con prefijo para guardar en BD
                        //return "data:image/png;base64," + generatedBase64;
                        return generatedBase64;
                    }
                }

                throw new ImageGenerationException("No se encontró imagen en la respuesta de Gemini");
            } else {
                throw new ImageGenerationException("No se recibió respuesta de Gemini");
            }

        } catch (Exception e) {
            log.error("❌ Error llamando a Gemini 2.5 Flash Image API", e);
            log.error("❌ Tipo de error: {}", e.getClass().getName());
            log.error("❌ Mensaje: {}", e.getMessage());
            if (e.getCause() != null) {
                log.error("❌ Causa: {}", e.getCause().getMessage());
            }
            throw new ImageGenerationException("Error generando imagen: " + e.getMessage(), e);
        } finally {
            // Cerrar VertexAI si es necesario
            // vertexAI.close(); // No cerrar aquí porque es reutilizable
        }
    }

    /**
     * Limpia el base64 removiendo prefijos
     */
    private String cleanBase64(String base64) {
        if (base64 == null) return null;
        if (base64.contains(",")) {
            return base64.substring(base64.indexOf(",") + 1);
        }
        return base64;
    }

    /**
     * Health check del servicio
     */
    public boolean checkHealth() {
        try {
            log.info("🏥 Verificando conexión con Gemini 2.5 Flash Image...");

            // Intentar crear el modelo como health check
            GenerativeModel model = new GenerativeModel(modelName, vertexAI);

            log.info("✅ Conexión con Gemini 2.5 Flash Image OK");
            return true;

        } catch (Exception e) {
            log.error("❌ Error conectando con Gemini 2.5 Flash Image", e);
            return false;
        }
    }
}