package org.paper.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateImageRequestDTO {

    @NotNull(message = "El ID del diseño es obligatorio")
    private Integer disenoId;

    @NotBlank(message = "El diseño en base64 es obligatorio")
    @Size(min = 100, message = "El diseño en base64 parece estar incompleto")
    private String base64Diseno;

    /**
     * Prompt adicional opcional para customizar la generación
     * Ejemplo: "Usa tonos más cálidos", "Enfoca el logo principal", etc.
     */
    @Size(max = 500, message = "El prompt adicional no puede superar los 500 caracteres")
    private String promptAdicional;
}