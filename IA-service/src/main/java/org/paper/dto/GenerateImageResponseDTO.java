package org.paper.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateImageResponseDTO {

    private Integer disenoId;
    private boolean success;
    private String message;
    private String errorDetails;

    public static GenerateImageResponseDTO success(Integer disenoId) {
        return GenerateImageResponseDTO.builder()
                .disenoId(disenoId)
                .success(true)
                .message("Imagen 3D generada exitosamente")
                .build();
    }

    public static GenerateImageResponseDTO error(Integer disenoId, String message, String errorDetails) {
        return GenerateImageResponseDTO.builder()
                .disenoId(disenoId)
                .success(false)
                .message(message)
                .errorDetails(errorDetails)
                .build();
    }
}