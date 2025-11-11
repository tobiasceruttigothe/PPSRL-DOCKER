package org.paper.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.paper.dto.GenerateImageRequestDTO;
import org.paper.dto.GenerateImageResponseDTO;
import org.paper.entity.Diseno;
import org.paper.exception.GeminiApiException;
import org.paper.repository.DisenoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio para generar imágenes 3D usando Stability AI
 * API Docs: https://platform.stability.ai/docs/api-reference
 */
@Slf4j
@Service
public class StabilityAIService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final DisenoRepository disenoRepository;

    //@Value("${stability.api.key}")
    private String apiKey = "sk-TnTJ2QtxMbSvdgADtgZMYMKAhVIInSY8uAtp2Z9kFOvvRNTv";

    //@Value("${stability.model:stable-diffusion-v1-6}")
    private String model = "stable-diffusion-v1-6";

    // Prompt optimizado para convertir diseños 2D en vistas 3D realistas
    private static final String BASE_PROMPT =
            "Professional 3D photorealistic render of a paper bag with this design. " +
                    "Studio lighting with soft shadows on a neutral white background. " +
                    "High quality product photography, 3/4 perspective view, commercial quality. " +
                    "The bag should look real and ready for e-commerce display.";

    public StabilityAIService(WebClient.Builder webClientBuilder,
                              ObjectMapper objectMapper,
                              DisenoRepository disenoRepository,
                              @Value("${stability.api.base-url:https://api.stability.ai}") String baseUrl) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
        this.disenoRepository = disenoRepository;
    }

    /**
     * Genera vista 3D y la guarda en la base de datos
     */
    @Transactional
    public GenerateImageResponseDTO generate3DImage(GenerateImageRequestDTO request) {
        log.info("Generando vista 3D para diseño ID: {}", request.getDisenoId());

        try {
            // 1. Buscar el diseño por ID
            Diseno diseno = disenoRepository.findById(request.getDisenoId())
                    .orElseThrow(() -> new GeminiApiException("Diseño no encontrado con ID: " + request.getDisenoId()));

            // 2. Validar que tenga una imagen base64
            if (diseno.getBase64Diseno() == null || diseno.getBase64Diseno().trim().isEmpty()) {
                throw new GeminiApiException("El diseño no contiene una imagen en base64");
            }

            // 3. Generar imagen 3D usando Stability AI
            String base64Image3D = generateImageWithStabilityAI(
                    diseno.getBase64Diseno(),
                    request.getPromptAdicional()
            );

            // 4. Guardar el resultado
            diseno.setBase64Vista3D(base64Image3D);
            diseno.setFechaActualizacion(LocalDateTime.now());
            disenoRepository.save(diseno);

            log.info("Vista 3D generada y guardada exitosamente para diseño ID: {}", request.getDisenoId());

            return GenerateImageResponseDTO.success(request.getDisenoId(), base64Image3D);

        } catch (WebClientResponseException e) {
            log.error("Error HTTP de Stability AI: {} - Body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return GenerateImageResponseDTO.error(
                    request.getDisenoId(),
                    "Error al generar imagen 3D",
                    parseStabilityError(e.getResponseBodyAsString())
            );

        } catch (Exception e) {
            log.error("Error inesperado al generar imagen 3D: {}", e.getMessage(), e);
            return GenerateImageResponseDTO.error(
                    request.getDisenoId(),
                    "Error inesperado al generar imagen 3D",
                    e.getMessage()
            );
        }
    }


    /**
     * Genera imagen usando Stability AI Image-to-Image
     */
    private String generateImageWithStabilityAI(String base64Input, String promptAdicional) {
        log.debug("Llamando a Stability AI para generar imagen 3D");

        // Limpiar prefijo data:image si existe
        String cleanBase64 = base64Input.replaceFirst("^data:image/[^;]+;base64,", "");

        // Combinar prompt base con adicional
        String fullPrompt = BASE_PROMPT;
        if (promptAdicional != null && !promptAdicional.trim().isEmpty()) {
            fullPrompt += " " + promptAdicional;
        }

        // Construir request para Stability AI (image-to-image)
        Map<String, Object> request = new HashMap<>();
        request.put("init_image", cleanBase64);
        request.put("init_image_mode", "IMAGE_STRENGTH");
        request.put("image_strength", 0.35); // Mantener el diseño original

        List<Map<String, Object>> textPrompts = List.of(
                Map.of(
                        "text", fullPrompt,
                        "weight", 1
                ),
                Map.of(
                        "text", "blurry, bad quality, distorted, flat, 2D, unrealistic",
                        "weight", -1 // Negative prompt
                )
        );
        request.put("text_prompts", textPrompts);

        request.put("cfg_scale", 7);
        request.put("samples", 1);
        request.put("steps", 30);

        try {
            String responseBody = webClient.post()
                    .uri("/v1/generation/" + model + "/image-to-image")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(120))
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                            .maxBackoff(Duration.ofSeconds(10))
                            .filter(throwable -> {
                                if (throwable instanceof WebClientResponseException) {
                                    int status = ((WebClientResponseException) throwable).getStatusCode().value();
                                    return status >= 500 || status == 429;
                                }
                                return false;
                            })
                            .doBeforeRetry(signal ->
                                    log.warn("Reintentando llamada a Stability AI (intento {}/3) - Error: {}",
                                            signal.totalRetries() + 1,
                                            signal.failure().getMessage())))
                    .block();

            return extractGeneratedImage(responseBody);

        } catch (Exception e) {
            log.error("Error en llamada a Stability AI: {}", e.getMessage(), e);
            throw new GeminiApiException("Error al comunicarse con Stability AI", e);
        }
    }

    /**
     * Extrae la imagen base64 del response de Stability AI
     */
    private String extractGeneratedImage(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);

            // Verificar errores
            if (root.has("message")) {
                String errorMsg = root.path("message").asText();
                throw new GeminiApiException("Stability AI error: " + errorMsg);
            }

            // Extraer imagen del array de artifacts
            JsonNode artifacts = root.path("artifacts");
            if (artifacts.isEmpty() || artifacts.isNull()) {
                throw new GeminiApiException("Respuesta de Stability AI sin artifacts");
            }

            String base64Image = artifacts.get(0).path("base64").asText();
            if (base64Image == null || base64Image.isEmpty()) {
                throw new GeminiApiException("No se encontró imagen en la respuesta");
            }

            log.info("Imagen extraída exitosamente (tamaño: ~{} KB)", base64Image.length() / 1024);
            return base64Image;

        } catch (Exception e) {
            log.error("Error al parsear respuesta de Stability AI: {}", e.getMessage(), e);
            throw new GeminiApiException("Error al procesar respuesta de Stability AI", e);
        }
    }

    /**
     * Parsea el mensaje de error de Stability AI
     */
    private String parseStabilityError(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            if (root.has("message")) {
                return root.path("message").asText("Error desconocido");
            }
        } catch (Exception e) {
            log.warn("No se pudo parsear error de Stability AI: {}", e.getMessage());
        }
        return "Error al comunicarse con Stability AI";
    }

    /**
     * Verifica la salud del servicio
     */
    public boolean checkHealth() {
        try {
            // Endpoint de balance para verificar conectividad
            String response = webClient.get()
                    .uri("/v1/user/balance")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();

            log.debug("Health check exitoso - Stability AI respondiendo");
            return true;

        } catch (Exception e) {
            log.error("Health check fallido: {}", e.getMessage());
            return false;
        }
    }
}