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

    @NotBlank(message = "La imagen base64 es obligatoria")
    private String imagenBase64;

    @Size(max = 500, message = "El prompt adicional no puede superar los 500 caracteres")
    private String promptAdicional;
}
