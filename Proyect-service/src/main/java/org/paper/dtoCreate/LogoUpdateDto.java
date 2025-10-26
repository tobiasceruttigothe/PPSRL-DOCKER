package org.paper.dtoCreate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogoUpdateDto {

    @NotBlank(message = "El nombre del logo es obligatorio")
    @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
    private String nombre;

    private String base64Logo; // Opcional: solo si se quiere cambiar la imagen
}