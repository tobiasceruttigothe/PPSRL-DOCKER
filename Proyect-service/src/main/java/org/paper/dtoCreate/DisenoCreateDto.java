package org.paper.dtoCreate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DisenoCreateDto {

    @NotNull(message = "El ID del usuario es obligatorio")
    private UUID usuarioId;

    @NotNull(message = "El ID de la plantilla es obligatorio")
    private Integer plantillaId;

    @NotBlank(message = "El nombre del diseño es obligatorio")
    @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
    private String nombre;

    @Size(max = 500, message = "La descripción no puede superar los 500 caracteres")
    private String descripcion;

    @NotBlank(message = "El diseño es obligatorio")
    private String base64Diseno;

    @NotBlank(message = "La vista previa dell diseño es obligatorio")
    private String base64Preview;
}