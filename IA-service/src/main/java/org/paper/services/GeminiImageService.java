package org.paper.services;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.*;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.paper.dto.GenerateImageResponseDTO;
import org.paper.entity.Diseno;
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
        log.info("🎨 Generando vista 3D para diseño ID: {}", disenoId);

        try {
            Diseno diseno = disenoRepository.findById(disenoId)
                    .orElseThrow(() -> new ImageGenerationException("Diseño no encontrado: " + disenoId));

            if (diseno.getBase64Preview() == null || diseno.getBase64Preview().isEmpty()) {
                throw new ImageGenerationException("El diseño no tiene imagen preview");
            }

            // 🔍 DETECTAR SI ES BOLSA O ENVOLTORIO
            String tipoBolsaNombre = diseno.getPlantilla().getTipoBolsa().getNombre().toLowerCase();
            boolean esBolsa = tipoBolsaNombre.contains("bolsa");
            boolean esEnvoltorio = tipoBolsaNombre.contains("envoltorio") || tipoBolsaNombre.contains("mantel");

            String dynamicPrompt;
            if (esBolsa) {
                log.info("📦 Tipo detectado: BOLSA → Usando prompt específico para bolsas");
                dynamicPrompt = buildPromptForBolsas(diseno);
            } else if (esEnvoltorio) {
                log.info("📄 Tipo detectado: ENVOLTORIO → Usando prompt específico para envoltorios");
                dynamicPrompt = buildPromptForEnvoltorios(diseno);
            } else {
                log.warn("⚠️ Tipo no reconocido: '{}'. Usando prompt genérico.", tipoBolsaNombre);
                dynamicPrompt = buildGenericPrompt(diseno);
            }

            log.debug("💬 Prompt enviado a Gemini:\n{}", dynamicPrompt);

            // 2. Generar imagen (Con Safety Settings relajados)
            String nuevaImagen3D = generateImageWithGemini(diseno.getBase64Preview(), dynamicPrompt);

            // 3. Guardar
            diseno.setBase64Preview(nuevaImagen3D);
            diseno.setFechaActualizacion(LocalDateTime.now());
            disenoRepository.save(diseno);

            log.info("✅ Imagen 3D generada exitosamente para diseño ID: {}", disenoId);
            return GenerateImageResponseDTO.success(disenoId);

        } catch (Exception e) {
            log.error("❌ Error generando vista 3D", e);
            return GenerateImageResponseDTO.error(disenoId, "Fallo Generación IA", e.getMessage());
        }
    }

    // ========================================
    // 📦 PROMPT PARA BOLSAS
    // ========================================
    private String buildPromptForBolsas(Diseno diseno) {
        Plantilla plantilla = diseno.getPlantilla();
        String tipoBolsaNombre = plantilla.getTipoBolsa().getNombre().toLowerCase();
        String materialNombre = plantilla.getMaterial().getNombre();

        // ✅ EXTRACCIÓN DE DIMENSIONES REALES
        String dimensionesReales = String.format("Width: %.1f cm, Height: %.1f cm, Depth (Gusset): %.1f cm",
                plantilla.getAncho(), plantilla.getAlto(), plantilla.getProfundidad());

        String geometryInstruction;

        // Lógica específica para tipos de bolsa
        if (tipoBolsaNombre.contains("americana") || tipoBolsaNombre.contains("fondo americano")) {
            geometryInstruction = """
                *CRITICAL GEOMETRY - PINCH BOTTOM BAG:*
                - TYPE: This is strictly a "Pinch Bottom Bag" (like a bakery bread bag).
                - BOTTOM: It has a *V-SHAPED pinch bottom*. It corresponds to a fold, NOT a flat rectangular cardboard base. It must look sharp at the bottom edge.
                - TOP: Serrated (zig-zag) cut edge. Open mouth.
                - SIDES: Deep gussets (indentations) on the sides.
                - STANCE: Standing upright, but slightly angled to show the depth.
                """;
        } else if (tipoBolsaNombre.contains("asa") || tipoBolsaNombre.contains("manija") || tipoBolsaNombre.contains("cuadrado con manija")) {
            geometryInstruction = """
                *CRITICAL GEOMETRY - SHOPPING BAG:*
                - TYPE: Rigid Shopping Bag with handles.
                - BOTTOM: Flat rectangular block bottom.
                - TOP: Folded straight edge (turn top).
                - HANDLES: Must have twisted paper or rope handles.
                """;
        } else if (tipoBolsaNombre.contains("cuadrado sin manija") || tipoBolsaNombre.contains("sin asa")) {
            geometryInstruction = """
                *CRITICAL GEOMETRY - SQUARE BOTTOM BAG (NO HANDLES):*
                - TYPE: Square bottom bag without handles.
                - BOTTOM: Flat square base.
                - TOP: Clean folded edge, no handles.
                - STANCE: Standing upright with visible side gussets.
                """;
        } else {
            geometryInstruction = """
                *GEOMETRY:*
                - Standard paper bag.
                - Define volume clearly using light and shadow.
                """;
        }

        return String.format("""
            You are an expert 3D packaging visualizer specialized in paper bags.
            
            *INPUT IMAGE ANALYSIS (CRITICAL):*
            The image provided is a *FLAT UNZIPPED TEMPLATE (DIELINE)*. It is NOT a sticker.
            You must mentally "fold" this image to construct the 3D object.
            
            *PHYSICAL SPECIFICATIONS (MUST RESPECT PROPORTIONS):*
            - *Dimensions:* %s
            
            *MAPPING INSTRUCTIONS:*
            1. *Center Panel:* The graphic in the exact center ("%s") goes on the *FRONT FACE*.
            2. *Side Panels:* The vertical strips on the far left and right correspond to the *SIDE GUSSETS*.
               -> *ACTION:* These vertical strips must appear on the *SIDE* of the bag, receding in perspective.
            
            %s

            *RENDERING SPECS:*
            - *Material:* %s (Render distinct paper fibers and texture).
            - *Lighting:* Studio lighting, emphasizing the fold lines and structure.
            - *Background:* Pure white.
            - *Camera Angle:* 3/4 Perspective view (showing Front and Side).

            Create a photorealistic product mockup based on these instructions.
            """,
                dimensionesReales,
                diseno.getNombre(),
                geometryInstruction,
                materialNombre
        );
    }

    // ========================================
    // 📄 PROMPT PARA ENVOLTORIOS
    // ========================================
    private String buildPromptForEnvoltorios(Diseno diseno) {
        Plantilla plantilla = diseno.getPlantilla();
        String tipoEnvoltorioNombre = plantilla.getTipoBolsa().getNombre().toLowerCase();
        String materialNombre = plantilla.getMaterial().getNombre();

        // ✅ EXTRACCIÓN DE DIMENSIONES (ANCHO x ALTO)
        // Asumimos que para envoltorios usamos ancho y alto como medidas de la hoja
        String dimensionesHoja = String.format("%.0f x %.0f cm", plantilla.getAncho(), plantilla.getAlto());

        return String.format("""
            You are an AI with expertise in packaging design and context understanding.
            
            *INPUT:* A flat design pattern for a wrapper.
            *TASK:* Render a photorealistic 3D mockup of this wrapper IN USE.
            
            *STEP 1: ANALYZE CONTEXT (THE "THINKING" PART)*
            Analyze the input image (logos, colors, text) AND the physical specs below to deduce what product is inside:
            - *Dimensions:* %s
            - *Material:* %s
            
            Logic Guide for your deduction:
            - IF dimensions are approx 30x30cm AND material is Greaseproof/Paraffin AND design implies burgers/fast-food -> *Wrap a Burger*.
            - IF dimensions are small (e.g., 10x10cm) AND design implies sweets/candy -> *Wrap a square/rectangular Candy* or taffy (twist wrap or fold).
            - IF dimensions are rectangular/large AND design implies bakery -> *Wrap a Baguette or Sandwich*.
            - IF it looks like a placemat (Individual) -> *Lay it flat* on a table surface with natural lighting.
            
            *STEP 2: RENDER*
            Based on your deduction in Step 1, generate the 3D image:
            - *The Product:* Show the paper wrapping the inferred invisible object (volumetric shape).
            - *Physics:* The paper must behave according to the material '%s'. 
              - If greaseproof: show slight translucency and grease resistance.
              - If kraft: show stiffness and fiber.
            - *Texture:* Add realistic crinkles, folds, and undulations appropriate for a wrapper.
            - *Design:* Map the provided flat design naturally over the folds and curves of the wrapped shape.
            
            *SCENE:*
            - Neutral studio background or wooden surface (if it fits the vibe).
            - Professional product photography style.
            """,
                diseno.getNombre(),
                dimensionesHoja,
                materialNombre
        );
    }

    // ========================================
    // 🔧 PROMPT GENÉRICO (FALLBACK)
    // ========================================
    private String buildGenericPrompt(Diseno diseno) {
        String materialNombre = diseno.getPlantilla().getMaterial().getNombre();

        return String.format("""
            You are an expert 3D product visualizer.
            
            *INPUT IMAGE:*
            The image provided is a flat design template for: "%s"
            
            *INSTRUCTIONS:*
            1. Analyze the structure and infer the best 3D representation.
            2. Apply the design faithfully to the appropriate surfaces.
            3. Use realistic lighting and material rendering for %s.
            4. Background: Pure white.
            5. Camera: 3/4 perspective view.
            
            Create a professional, photorealistic product mockup.
            """,
                diseno.getNombre(),
                materialNombre
        );
    }

    // ========================================
    // 🤖 LLAMADA A GEMINI (Sin cambios)
    // ========================================
    private String generateImageWithGemini(String base64Preview, String prompt) {
        try {
            String cleanBase64 = base64Preview.contains(",") ?
                    base64Preview.substring(base64Preview.indexOf(",") + 1) : base64Preview;

            byte[] imageBytes = Base64.getDecoder().decode(cleanBase64);

            List<SafetySetting> safetySettings = Arrays.asList(
                    SafetySetting.newBuilder().setCategory(HarmCategory.HARM_CATEGORY_HARASSMENT).setThreshold(SafetySetting.HarmBlockThreshold.BLOCK_ONLY_HIGH).build(),
                    SafetySetting.newBuilder().setCategory(HarmCategory.HARM_CATEGORY_HATE_SPEECH).setThreshold(SafetySetting.HarmBlockThreshold.BLOCK_ONLY_HIGH).build(),
                    SafetySetting.newBuilder().setCategory(HarmCategory.HARM_CATEGORY_SEXUALLY_EXPLICIT).setThreshold(SafetySetting.HarmBlockThreshold.BLOCK_ONLY_HIGH).build(),
                    SafetySetting.newBuilder().setCategory(HarmCategory.HARM_CATEGORY_DANGEROUS_CONTENT).setThreshold(SafetySetting.HarmBlockThreshold.BLOCK_ONLY_HIGH).build()
            );

            GenerativeModel model = new GenerativeModel(modelName, vertexAI)
                    .withGenerationConfig(GenerationConfig.newBuilder()
                            .setTemperature(0.3f)
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

                if (candidate.getFinishReason() == Candidate.FinishReason.SAFETY) {
                    log.error("⛔ IMAGEN BLOQUEADA POR FILTROS DE SEGURIDAD. Ratings: {}", candidate.getSafetyRatingsList());
                    throw new ImageGenerationException("La IA bloqueó la generación por motivos de seguridad.");
                }

                if (candidate.getContent().getPartsCount() > 0) {
                    for (Part part : candidate.getContent().getPartsList()) {
                        if (part.hasInlineData()) {
                            return Base64.getEncoder().encodeToString(part.getInlineData().getData().toByteArray());
                        }
                    }
                }

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