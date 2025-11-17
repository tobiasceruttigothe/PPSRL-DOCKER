package org.paper.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request para generar vista 3D
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateImageRequestDTO {

    @NotNull(message = "El ID del diseño es obligatorio")
    private Integer disenoId;
}