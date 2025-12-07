package org.paper.services;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.*;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.paper.dto.GenerateImageResponseDTO;
import org.paper.entity.Diseno;
import org.paper.entity.Material;
import org.paper.entity.Plantilla;
import org.paper.exception.ImageGenerationException;
import org.paper.repository.DisenoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

@Slf4j
@Service
public class GeminiImageService {

    private final DisenoRepository disenoRepository;
    private final String modelName;
    private final VertexAI vertexAI;

    public GeminiImageService(
            DisenoRepository disenoRepository,
            @Value("${google.cloud.project-id}") String projectId,
            @Value("${google.cloud.location}") String location,
            @Value("${gemini.model-name:gemini-2.5-flash-image}") String modelName) {
        this.disenoRepository = disenoRepository;
        this.modelName = modelName;
        this.vertexAI = new VertexAI(projectId, location);
    }

    @Transactional
    public GenerateImageResponseDTO generate3DView(Integer disenoId) {
        log.info(" Generando vista 3D (V3 - Pinch Bottom Logic) para diseño ID: {}", disenoId);

        try {
            Diseno diseno = disenoRepository.findById(disenoId)
                    .orElseThrow(() -> new ImageGenerationException("Diseño no encontrado: " + disenoId));

            if (diseno.getBase64Preview() == null || diseno.getBase64Preview().isEmpty()) {
                throw new ImageGenerationException("El diseño no tiene imagen preview");
            }

            // 1. Construir Prompt con lógica de geometría mejorada
            String dynamicPrompt = buildAdvancedPrompt(diseno);
            log.info(" Prompt Enviado a Gemini:\n{}", dynamicPrompt);

            // 2. Generar imagen (Con Safety Settings relajados)
            String nuevaImagen3D = generateImageWithGemini(diseno.getBase64Preview(), dynamicPrompt);

            // 3. Guardar
            diseno.setBase64Preview(nuevaImagen3D);
            diseno.setFechaActualizacion(LocalDateTime.now());
            disenoRepository.save(diseno);

            return GenerateImageResponseDTO.success(disenoId);

        } catch (Exception e) {
            log.error("❌ Error generando vista 3D", e);
            return GenerateImageResponseDTO.error(disenoId, "Fallo Generación IA", e.getMessage());
        }
    }

    private String buildAdvancedPrompt(Diseno diseno) {
        Plantilla plantilla = diseno.getPlantilla();
        // Usamos toLowerCase para asegurar que el match funcione sin importar mayúsculas en la BD
        String tipoBolsaNombre = plantilla.getTipoBolsa().getNombre().toLowerCase();
        String materialNombre = plantilla.getMaterial().getNombre();

        String geometryInstruction;

        // Lógica de selección de geometría
        if (tipoBolsaNombre.contains("americana")) {
            // PROMPT ESPECÍFICO: PINCH BOTTOM (FONDO EN V)
            geometryInstruction = """
                **CRITICAL GEOMETRY - PINCH BOTTOM BAG:**
                - TYPE: This is strictly a "Pinch Bottom Bag" (like a bakery bread bag).
                - BOTTOM: It has a **V-SHAPED pinch bottom**. It corresponds to a fold, NOT a flat rectangular cardboard base. It must look sharp at the bottom edge.
                - TOP: Serrated (zig-zag) cut edge. Open mouth.
                - SIDES: Deep gussets (indentations) on the sides.
                - STANCE: Standing upright, but slightly angled to show the depth.
                """;
        } else if (tipoBolsaNombre.contains("asa") || tipoBolsaNombre.contains("manija")) {
            // PROMPT ESPECÍFICO: SHOPPING BAG
            geometryInstruction = """
                **CRITICAL GEOMETRY - SHOPPING BAG:**
                - TYPE: Rigid Shopping Bag with handles.
                - BOTTOM: Flat rectangular block bottom.
                - TOP: Folded straight edge (turn top).
                - HANDLES: Must have twisted paper or rope handles.
                """;
        } else {
            // GENÉRICO
            geometryInstruction = """
                **GEOMETRY:**
                - Standard paper bag.
                - Define volume clearly using light and shadow.
                """;
        }

        // El Prompt Maestro
        return String.format("""
            You are an expert 3D packaging visualizer.
            
            **INPUT IMAGE ANALYSIS (CRITICAL):**
            The image provided is a **FLAT UNZIPPED TEMPLATE (DIELINE)**. It is NOT a sticker to be placed on the front.
            You must mentally "fold" this image to construct the 3D object.
            
            **MAPPING INSTRUCTIONS:**
            1. **Center Panel:** The graphic in the exact center ("%s") goes on the **FRONT FACE** of the bag.
            2. **Side Panels:** The vertical strips on the far left and far right (containing vertical text) correspond to the **SIDE GUSSETS**.
               -> **ACTION:** In your 3D render, this vertical text must appear on the **SIDE** of the bag, receding in perspective. It should NOT be visible on the front face.
            
            %s

            **RENDERING SPECS:**
            - **Material:** %s. (Render distinct paper fibers and texture).
            - **Lighting:** Studio lighting, emphasizing the fold lines and the "V" shape of the bottom (if applicable).
            - **Background:** Pure white.
            - **Camera Angle:** 3/4 Perspective view (showing Front and Side).

            Create a photorealistic product mockup based on these instructions.
            """,
                diseno.getNombre(), // Nombre del diseño para referencia
                geometryInstruction,
                materialNombre
        );
    }

    private String generateImageWithGemini(String base64Preview, String prompt) {
        try {
            String cleanBase64 = base64Preview.contains(",") ?
                    base64Preview.substring(base64Preview.indexOf(",") + 1) : base64Preview;

            byte[] imageBytes = Base64.getDecoder().decode(cleanBase64);

            // SAFETY SETTINGS: Bajamos los filtros para permitir marcas/logos (soluciona el error en logs)
            List<SafetySetting> safetySettings = Arrays.asList(
                    SafetySetting.newBuilder().setCategory(HarmCategory.HARM_CATEGORY_HARASSMENT).setThreshold(SafetySetting.HarmBlockThreshold.BLOCK_ONLY_HIGH).build(),
                    SafetySetting.newBuilder().setCategory(HarmCategory.HARM_CATEGORY_HATE_SPEECH).setThreshold(SafetySetting.HarmBlockThreshold.BLOCK_ONLY_HIGH).build(),
                    SafetySetting.newBuilder().setCategory(HarmCategory.HARM_CATEGORY_SEXUALLY_EXPLICIT).setThreshold(SafetySetting.HarmBlockThreshold.BLOCK_ONLY_HIGH).build(),
                    SafetySetting.newBuilder().setCategory(HarmCategory.HARM_CATEGORY_DANGEROUS_CONTENT).setThreshold(SafetySetting.HarmBlockThreshold.BLOCK_ONLY_HIGH).build()
            );

            GenerativeModel model = new GenerativeModel(modelName, vertexAI)
                    .withGenerationConfig(GenerationConfig.newBuilder()
                            .setTemperature(0.3f) // Temperatura más baja = más fiel a las instrucciones geométricas
                            .setMaxOutputTokens(8192)
                            .build())
                    .withSafetySettings(safetySettings);

            Content content = Content.newBuilder()
                    .setRole("user")
                    .addParts(Part.newBuilder()
                            .setInlineData(com.google.cloud.vertexai.api.Blob.newBuilder()
                                    .setMimeType("image/png")
                                    .setData(ByteString.copyFrom(imageBytes))
                                    .build()))
                    .addParts(Part.newBuilder().setText(prompt).build())
                    .build();

            GenerateContentResponse response = model.generateContent(content);

            if (response.getCandidatesCount() > 0) {
                var candidate = response.getCandidates(0);

                // Detección explicita de bloqueo por seguridad
                if (candidate.getFinishReason() == Candidate.FinishReason.SAFETY) {
                    log.error("⛔ IMAGEN BLOQUEADA POR FILTROS DE SEGURIDAD (Copyright/Marca). Ratings: {}", candidate.getSafetyRatingsList());
                    throw new ImageGenerationException("La IA bloqueó la generación por motivos de seguridad o derechos de marca.");
                }

                if (candidate.getContent().getPartsCount() > 0) {
                    for (Part part : candidate.getContent().getPartsList()) {
                        if (part.hasInlineData()) {
                            return Base64.getEncoder().encodeToString(part.getInlineData().getData().toByteArray());
                        }
                    }
                }

                // Si llegamos aquí, respondió texto (explicando por qué no generó imagen)
                String textResponse = candidate.getContent().getPartsCount() > 0 ?
                        candidate.getContent().getParts(0).getText() : "Sin respuesta de texto";
                log.warn("⚠️ Gemini devolvió texto en lugar de imagen: {}", textResponse);
                throw new ImageGenerationException("Gemini se negó a generar la imagen: " + textResponse);
            }

            throw new ImageGenerationException("Error desconocido en la respuesta de Gemini");

        } catch (Exception e) {
            log.error("Excepción técnica en Vertex AI", e);
            throw new ImageGenerationException("Error al conectar con el servicio de IA: " + e.getMessage(), e);
        }
    }

    public boolean checkHealth() {
        try {
            new GenerativeModel(modelName, vertexAI);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}