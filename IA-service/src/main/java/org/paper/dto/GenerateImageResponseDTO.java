package org.paper.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
//@antml:parameter>
@AllArgsConstructor
public class GenerateImageResponseDTO {

    private Integer disenoId;
    private String base64Vista3D;
    private boolean success;
    private String message;
    private String errorDetails;

    // Constructor de conveniencia para respuestas exitosas
    public static GenerateImageResponseDTO success(Integer disenoId, String base64Vista3D) {
        return GenerateImageResponseDTO.builder()
                .disenoId(disenoId)
                .base64Vista3D(base64Vista3D)
                .success(true)
                .message("Imagen 3D generada exitosamente")
                .build();
    }

    // Constructor de conveniencia para errores
    public static GenerateImageResponseDTO error(Integer disenoId, String message, String errorDetails) {
        return GenerateImageResponseDTO.builder()
                .disenoId(disenoId)
                .success(false)
                .message(message)
                .errorDetails(errorDetails)
                .build();
    }
}