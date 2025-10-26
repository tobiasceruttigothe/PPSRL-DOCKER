package org.paper.dtoCreate;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlantillaUpdateDto {

    @NotBlank(message = "El nombre de la plantilla es obligatorio")
    @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
    private String nombre;

    @NotNull(message = "El ID del material es obligatorio")
    private Integer materialId;

    @NotNull(message = "El ID del tipo de bolsa es obligatorio")
    private Integer tipoBolsaId;

    // Base64 opcional: solo si se quiere cambiar la imagen
    private String base64Plantilla;

    @NotNull(message = "El ancho es obligatorio")
    @Positive(message = "El ancho debe ser mayor a 0")
    private Float ancho;

    @NotNull(message = "El alto es obligatorio")
    @Positive(message = "El alto debe ser mayor a 0")
    private Float alto;

    @NotNull(message = "La profundidad es obligatoria")
    @Positive(message = "La profundidad debe ser mayor a 0")
    private Float profundidad;
}