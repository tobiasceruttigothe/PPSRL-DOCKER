package org.paper.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.paper.dto.GenerateImageRequestDTO;
import org.paper.dto.GenerateImageResponseDTO;
import org.paper.exception.GeminiApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class GeminiService {

    private final WebClient geminiWebClient;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.model}")
    private String model;

    // Prompt preestablecido optimizado para generación de bolsas 3D
    private static final String SYSTEM_PROMPT = """
        Eres un diseñador 3D experto especializado en packaging y bolsas de papel.
        Tu tarea es generar una imagen 3D profesional, realista y de alta calidad de una bolsa
        basándote en el diseño vectorizado que te proporciono.
        
        REQUISITOS OBLIGATORIOS:
        - La imagen debe ser fotorrealista con iluminación profesional de estudio
        - Mostrar la bolsa en perspectiva 3D (vista en ángulo 3/4)
        - Incluir sombras suaves y difusas en la base
        - Texturas realistas del material (papel kraft, cartón, etc.)
        - Mantener TODOS los elementos del diseño original (logos, textos, colores, formas)
        - Fondo neutro blanco o gris muy claro para destacar el producto
        - Resolución alta y detalles nítidos
        - La bolsa debe verse como un producto real listo para fotografía comercial
        - Simular el volumen y la tridimensionalidad del empaque
        
        IMPORTANTE:
        - NO añadas elementos que no estén en el diseño original
        - NO modifiques colores ni logos
        - NO incluyas texto adicional ni marcas de agua
        - La bolsa debe verse profesional y lista para venta
        
        Genera ÚNICAMENTE la imagen, sin texto ni explicaciones.
        """;

    public GeminiService(WebClient geminiWebClient, ObjectMapper objectMapper) {
        this.geminiWebClient = geminiWebClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Genera una imagen 3D usando Google Gemini
     */
    public GenerateImageResponseDTO generate3DImage(GenerateImageRequestDTO request) {
        log.info("Iniciando generación 3D para diseño ID: {}", request.getDisenoId());

        try {
            // Validar que la imagen no esté vacía
            if (request.getBase64Diseno() == null || request.getBase64Diseno().trim().isEmpty()) {
                throw new GeminiApiException("El diseño en base64 está vacío");
            }

            // Construir el request para Gemini
            Map<String, Object> geminiRequest = buildGeminiRequest(
                    request.getBase64Diseno(),
                    request.getPromptAdicional()
            );

            log.debug("Request construido para Gemini, enviando a API...");

            // Llamar a Gemini API con retry
            String responseBody = callGeminiApi(geminiRequest);

            log.debug("Respuesta recibida de Gemini, extrayendo imagen...");

            // Extraer la imagen generada del response
            String base64Image = extractGeneratedImage(responseBody);

            // Validar que la imagen no esté vacía
            if (base64Image == null || base64Image.trim().isEmpty()) {
                throw new GeminiApiException("La imagen generada está vacía");
            }

            log.info("Imagen 3D generada exitosamente para diseño ID: {} (tamaño: ~{} KB)",
                    request.getDisenoId(),
                    base64Image.length() / 1024);

            return GenerateImageResponseDTO.builder()
                    .disenoId(request.getDisenoId())
                    .base64Vista3D(base64Image)
                    .success(true)
                    .message("Imagen 3D generada exitosamente")
                    .build();

        } catch (WebClientResponseException e) {
            log.error("Error HTTP de Gemini API: {} - Body: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());

            String errorMsg = parseGeminiError(e.getResponseBodyAsString());

            return GenerateImageResponseDTO.builder()
                    .disenoId(request.getDisenoId())
                    .success(false)
                    .message("Error al generar imagen 3D")
                    .errorDetails(errorMsg)
                    .build();

        } catch (GeminiApiException e) {
            log.error("Error de negocio al generar imagen 3D: {}", e.getMessage(), e);

            return GenerateImageResponseDTO.builder()
                    .disenoId(request.getDisenoId())
                    .success(false)
                    .message("Error al procesar la imagen")
                    .errorDetails(e.getMessage())
                    .build();

        } catch (Exception e) {
            log.error("Error inesperado al generar imagen 3D: {}", e.getMessage(), e);

            return GenerateImageResponseDTO.builder()
                    .disenoId(request.getDisenoId())
                    .success(false)
                    .message("Error inesperado al generar imagen 3D")
                    .errorDetails(e.getMessage())
                    .build();
        }
    }

    /**
     * Construye el request para la API de Gemini
     */
    private Map<String, Object> buildGeminiRequest(String base64Image, String promptAdicional) {
        // Limpiar prefijo data:image si existe
        String cleanBase64 = base64Image.replaceFirst("^data:image/[^;]+;base64,", "");

        // Combinar prompt del sistema con prompt adicional si existe
        String fullPrompt = SYSTEM_PROMPT;
        if (promptAdicional != null && !promptAdicional.trim().isEmpty()) {
            fullPrompt += "\n\nINSTRUCCIONES ADICIONALES DEL USUARIO:\n" + promptAdicional;
        }

        // Estructura del request según la API de Gemini
        Map<String, Object> request = new HashMap<>();

        // Contents array
        Map<String, Object> content = new HashMap<>();
        content.put("role", "user");

        // Parts array (texto + imagen)
        List<Map<String, Object>> parts = List.of(
                Map.of("text", fullPrompt),
                Map.of("inline_data", Map.of(
                        "mime_type", "image/png",
                        "data", cleanBase64
                ))
        );
        content.put("parts", parts);

        request.put("contents", List.of(content));

        // Configuración de generación optimizada para imágenes
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.4);  // Más determinista para resultados consistentes
        generationConfig.put("topK", 32);
        generationConfig.put("topP", 1);
        generationConfig.put("maxOutputTokens", 4096); // Mayor para imágenes de alta calidad
        request.put("generationConfig", generationConfig);

        // Safety settings (opcional, para evitar filtros muy estrictos)
        List<Map<String, Object>> safetySettings = List.of(
                Map.of("category", "HARM_CATEGORY_HARASSMENT", "threshold", "BLOCK_NONE"),
                Map.of("category", "HARM_CATEGORY_HATE_SPEECH", "threshold", "BLOCK_NONE"),
                Map.of("category", "HARM_CATEGORY_SEXUALLY_EXPLICIT", "threshold", "BLOCK_NONE"),
                Map.of("category", "HARM_CATEGORY_DANGEROUS_CONTENT", "threshold", "BLOCK_NONE")
        );
        request.put("safetySettings", safetySettings);

        return request;
    }

    /**
     * Llama a la API de Gemini con retry automático
     */
    private String callGeminiApi(Map<String, Object> request) {
        String endpoint = String.format("/v1beta/models/%s:generateContent", model);

        log.debug("Llamando a Gemini API: {}", endpoint);

        return geminiWebClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path(endpoint)
                        .queryParam("key", apiKey)
                        .build())
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(120)) // Timeout de 2 minutos para generación de imágenes
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .maxBackoff(Duration.ofSeconds(10))
                        .filter(throwable -> {
                            if (throwable instanceof WebClientResponseException) {
                                int status = ((WebClientResponseException) throwable).getStatusCode().value();
                                // Reintentar solo en errores 5xx (servidor) o 429 (rate limit)
                                return status >= 500 || status == 429;
                            }
                            return false;
                        })
                        .doBeforeRetry(signal ->
                                log.warn("Reintentando llamada a Gemini (intento {}/3) - Error: {}",
                                        signal.totalRetries() + 1,
                                        signal.failure().getMessage())))
                .block();
    }

    /**
     * Extrae la imagen base64 del response de Gemini
     */
    private String extractGeneratedImage(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);

            log.debug("Parseando respuesta de Gemini...");

            // Verificar si hay error en la respuesta
            if (root.has("error")) {
                String errorMsg = root.path("error").path("message").asText();
                log.error("Error en respuesta de Gemini: {}", errorMsg);
                throw new GeminiApiException("Gemini API error: " + errorMsg);
            }

            // Navegar por la estructura del response de Gemini
            JsonNode candidates = root.path("candidates");
            if (candidates.isEmpty() || candidates.isNull()) {
                log.error("Respuesta de Gemini sin candidates. Response: {}", responseBody);
                throw new GeminiApiException("Respuesta de Gemini sin candidates");
            }

            JsonNode content = candidates.get(0).path("content");
            if (content.isEmpty() || content.isNull()) {
                log.error("Respuesta de Gemini sin content");
                throw new GeminiApiException("Respuesta de Gemini sin content");
            }

            JsonNode parts = content.path("parts");
            if (parts.isEmpty() || parts.isNull()) {
                log.error("Respuesta de Gemini sin parts");
                throw new GeminiApiException("Respuesta de Gemini sin parts");
            }

            // Buscar la parte que contiene la imagen
            for (JsonNode part : parts) {
                if (part.has("inline_data")) {
                    String base64Image = part.path("inline_data").path("data").asText();
                    if (base64Image != null && !base64Image.isEmpty()) {
                        log.info("Imagen extraída exitosamente (tamaño: ~{} KB)",
                                base64Image.length() / 1024);
                        return base64Image;
                    }
                }

                // También buscar en "text" por si Gemini devuelve el base64 ahí
                if (part.has("text")) {
                    String text = part.path("text").asText();
                    // Validar si el texto parece un base64 válido
                    if (text != null && text.length() > 1000 && isLikelyBase64(text)) {
                        log.info("Imagen encontrada en campo 'text' (tamaño: ~{} KB)",
                                text.length() / 1024);
                        return text;
                    }
                }
            }

            log.error("No se encontró imagen en la respuesta de Gemini. Response structure: {}",
                    root.toPrettyString());
            throw new GeminiApiException("No se encontró imagen en la respuesta de Gemini");

        } catch (Exception e) {
            log.error("Error al parsear respuesta de Gemini: {}", e.getMessage(), e);
            throw new GeminiApiException("Error al procesar respuesta de Gemini", e);
        }
    }

    /**
     * Verifica si un string parece ser base64
     */
    private boolean isLikelyBase64(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        // Base64 solo contiene A-Z, a-z, 0-9, +, /, =
        return str.matches("^[A-Za-z0-9+/]+=*$");
    }

    /**
     * Parsea el mensaje de error de Gemini
     */
    private String parseGeminiError(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            if (root.has("error")) {
                return root.path("error").path("message").asText("Error desconocido");
            }
        } catch (Exception e) {
            log.warn("No se pudo parsear error de Gemini: {}", e.getMessage());
        }
        return "Error al comunicarse con Gemini API";
    }

    /**
     * Verifica la salud del servicio
     */
    public boolean checkHealth() {
        try {
            // Hacer una llamada simple a Gemini para verificar conectividad
            String endpoint = String.format("/v1beta/models/%s", model);

            String response = geminiWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(endpoint)
                            .queryParam("key", apiKey)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();

            log.debug("Health check exitoso - Gemini API respondiendo. Model info: {}",
                    response != null ? response.substring(0, Math.min(100, response.length())) : "null");
            return true;

        } catch (Exception e) {
            log.error("Health check fallido: {}", e.getMessage());
            return false;
        }
    }
}