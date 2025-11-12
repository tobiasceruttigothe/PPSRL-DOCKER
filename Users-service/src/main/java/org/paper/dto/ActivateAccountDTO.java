package org.paper.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para activación de cuenta de usuario.
 * Combina verificación de email + establecimiento de contraseña definitiva.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivateAccountDTO {

    @NotBlank(message = "El token es obligatorio")
    private String token;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 50, message = "La contraseña debe tener entre 8 y 50 caracteres")
    private String newPassword;
}