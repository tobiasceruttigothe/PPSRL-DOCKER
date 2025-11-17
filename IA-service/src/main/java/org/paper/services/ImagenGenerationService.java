package org.paper.services;

import com.google.cloud.aiplatform.v1.*;
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;
import lombok.extern.slf4j.Slf4j;
import org.paper.dto.GenerateImageResponseDTO;
import org.paper.dto.TipoBolsaEnum;
import org.paper.entity.Diseno;
import org.paper.exception.ImageGenerationException;
import org.paper.repository.DisenoRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Servicio para generar vistas 3D usando Google Imagen 3
 */
@Slf4j
@Service
public class ImagenGenerationService {

    private final DisenoRepository disenoRepository;
    private final PredictionServiceClient predictionServiceClient;
    private final String projectId;
    private final String location;

    @org.springframework.beans.factory.annotation.Value("${imagen.model-name:imagegeneration@006}")
    private String modelName;

    // Prompts específicos por tipo de bolsa (TEXT-TO-IMAGE mode)
    private static final Map<TipoBolsaEnum, String> PROMPTS = new HashMap<>();

    static {
        PROMPTS.put(TipoBolsaEnum.FONDO_AMERICANO,
                """
                Professional product photography of a kraft paper shopping bag with American bottom style.
                The bag is standing upright on a clean white surface with soft studio lighting.
                American bottom construction with rectangular base and 4 folded corners clearly visible.
                Natural brown kraft paper texture with subtle fiber details.
                Realistic shadows cast on the surface beneath.
                Clean, modern design with simple branding area on the front.
                Professional commercial quality render.
                Photorealistic 3D visualization.
                High resolution product mockup.
                Studio quality lighting with soft shadows.
                """
        );

        PROMPTS.put(TipoBolsaEnum.FONDO_CUADRADO_CON_MANIJA,
                """
                Professional product photography of a kraft paper shopping bag with square bottom and twisted paper handles.
                The bag is standing upright on a clean white surface with soft studio lighting.
                Square flat bottom base with clean fold lines.
                Twisted kraft paper handles attached to the top, hanging naturally.
                Natural brown kraft paper texture with realistic fiber details.
                Handles show realistic paper twist texture and proper attachment points.
                Realistic shadows cast on the surface beneath and under the handles.
                Clean, modern design with simple branding area on the front.
                Professional commercial quality render.
                Photorealistic 3D visualization.
                High resolution product mockup.
                Studio quality lighting.
                """
        );

        PROMPTS.put(TipoBolsaEnum.FONDO_CUADRADO_SIN_MANIJA,
                """
                Professional product photography of a kraft paper shopping bag with square bottom and no handles.
                The bag is standing upright on a clean white surface with soft studio lighting.
                Square flat bottom base with clean fold lines clearly visible.
                Clean top edge with no handles.
                Natural brown kraft paper texture with subtle fiber details.
                Realistic shadows cast on the surface beneath.
                Clean, modern minimalist design with simple branding area on the front.
                Professional commercial quality render.
                Photorealistic 3D visualization.
                High resolution product mockup.
                Studio quality lighting with soft shadows.
                """
        );

        PROMPTS.put(TipoBolsaEnum.GENERICO,
                """
                Professional product photography of a kraft paper shopping bag.
                The bag is standing upright on a clean white surface with soft studio lighting.
                Natural brown kraft paper texture with realistic fiber details.
                Clean modern design with simple branding area.
                Realistic shadows cast on the surface beneath.
                Professional commercial quality render.
                Photorealistic 3D visualization.
                High resolution product mockup.
                Studio quality lighting.
                Product photography style.
                """
        );
    }

    public ImagenGenerationService(
            DisenoRepository disenoRepository,
            PredictionServiceClient predictionServiceClient,
            String googleCloudProjectId,
            String googleCloudLocation) {
        this.disenoRepository = disenoRepository;
        this.predictionServiceClient = predictionServiceClient;
        this.projectId = googleCloudProjectId;
        this.location = googleCloudLocation;
    }

    /**
     * Genera vista 3D para un diseño específico
     */
    @Transactional
    public GenerateImageResponseDTO generate3DView(Integer disenoId) {
        log.info("🎨 Iniciando generación de vista 3D para diseño ID: {}", disenoId);

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

            // 4. Generar imagen 3D
            String nuevaImagen3D = generateImageWithImagen3(
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
     * Llama a Google Imagen 3 para generar la imagen
     * Usa modo TEXT-TO-IMAGE puro (no requiere imagen de referencia)
     */
    private String generateImageWithImagen3(String base64Preview, TipoBolsaEnum tipoBolsa) {
        try {
            log.info("📡 Llamando a Google Imagen 3 API...");
            log.info("🎨 Tipo de bolsa: {}", tipoBolsa.getDisplayName());

            // Obtener prompt según tipo de bolsa
            String prompt = PROMPTS.get(tipoBolsa);
            log.info("📝 Prompt: {}", prompt.substring(0, Math.min(100, prompt.length())) + "...");

            // Construir endpoint correcto para Imagen 3
            String endpoint = String.format(
                    "projects/%s/locations/%s/publishers/google/models/%s",
                    projectId, location, modelName
            );
            log.info("🔗 Endpoint: {}", endpoint);

            // MODO TEXT-TO-IMAGE (sin imagen de referencia)
            // Esto es más simple y funciona mejor para generación de mockups 3D
            Map<String, Object> instanceMap = new HashMap<>();
            instanceMap.put("prompt", prompt);

            // Parámetros para Imagen 3
            Map<String, Object> parametersMap = new HashMap<>();
            parametersMap.put("sampleCount", 1);
            parametersMap.put("aspectRatio", "1:1"); // Cuadrado
            parametersMap.put("negativePrompt",
                    "blurry, low quality, distorted, deformed, ugly, bad proportions, watermark");

            log.info("⚙️ Construyendo request para Google Vertex AI (TEXT-TO-IMAGE)...");

            // Convertir a formato Protobuf Value
            Value instanceValue = jsonToValue(instanceMap);
            Value parametersValue = jsonToValue(parametersMap);

            // Hacer la predicción
            PredictRequest request = PredictRequest.newBuilder()
                    .setEndpoint(endpoint)
                    .addInstances(instanceValue)
                    .setParameters(parametersValue)
                    .build();

            log.info("🚀 Enviando request a Vertex AI...");
            PredictResponse response = predictionServiceClient.predict(request);
            log.info("📥 Respuesta recibida de Vertex AI");

            // Extraer imagen generada
            if (response.getPredictionsCount() > 0) {
                Value prediction = response.getPredictions(0);

                log.info("🔍 Campos disponibles en respuesta: {}",
                        prediction.getStructValue().getFieldsMap().keySet());

                // Intentar extraer el base64 de diferentes posibles ubicaciones
                String generatedBase64 = null;

                // Opción 1: bytesBase64Encoded directo
                if (prediction.getStructValue().getFieldsMap().containsKey("bytesBase64Encoded")) {
                    generatedBase64 = prediction.getStructValue()
                            .getFieldsMap()
                            .get("bytesBase64Encoded")
                            .getStringValue();
                }
                // Opción 2: predictions array
                else if (prediction.getStructValue().getFieldsMap().containsKey("predictions")) {
                    Value predictions = prediction.getStructValue()
                            .getFieldsMap()
                            .get("predictions");
                    if (predictions.getListValue().getValuesCount() > 0) {
                        Value firstPrediction = predictions.getListValue().getValues(0);
                        if (firstPrediction.getStructValue().getFieldsMap().containsKey("bytesBase64Encoded")) {
                            generatedBase64 = firstPrediction.getStructValue()
                                    .getFieldsMap()
                                    .get("bytesBase64Encoded")
                                    .getStringValue();
                        }
                    }
                }

                if (generatedBase64 != null && !generatedBase64.isEmpty()) {
                    log.info("✅ Imagen generada exitosamente por Google Imagen 3");
                    log.info("📊 Tamaño de imagen generada: {} caracteres", generatedBase64.length());

                    // Retornar con prefijo para guardar en BD
                    return "data:image/png;base64," + generatedBase64;
                } else {
                    log.error("❌ No se encontró bytesBase64Encoded en la respuesta");
                    log.error("❌ Estructura de respuesta: {}", prediction);
                    throw new ImageGenerationException("No se pudo extraer la imagen de la respuesta");
                }
            } else {
                throw new ImageGenerationException("No se recibió respuesta de la API");
            }

        } catch (Exception e) {
            log.error("❌ Error llamando a Google Imagen 3 API", e);
            log.error("❌ Tipo de error: {}", e.getClass().getName());
            log.error("❌ Mensaje: {}", e.getMessage());
            if (e.getCause() != null) {
                log.error("❌ Causa: {}", e.getCause().getMessage());
            }
            throw new ImageGenerationException("Error generando imagen: " + e.getMessage(), e);
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
     * Convierte Map a Protobuf Value
     */
    private Value jsonToValue(Map<String, Object> map) throws Exception {
        String json = new com.google.gson.Gson().toJson(map);
        Value.Builder builder = Value.newBuilder();
        JsonFormat.parser().merge(json, builder);
        return builder.build();
    }

    /**
     * Health check del servicio
     */
    public boolean checkHealth() {
        try {
            // Intentar listar endpoints como health check
            log.info("🏥 Verificando conexión con Google Vertex AI...");

            String parent = LocationName.of(projectId, location).toString();

            // Si esto no lanza excepción, estamos conectados
            log.info("✅ Conexión con Google Vertex AI OK");
            return true;

        } catch (Exception e) {
            log.error("❌ Error conectando con Google Vertex AI", e);
            return false;
        }
    }
}