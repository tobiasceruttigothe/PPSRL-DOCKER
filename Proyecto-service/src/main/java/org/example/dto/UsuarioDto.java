package org.example.dto;

import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO (Data Transfer Object) para la entidad Usuario.
 * Se usa para transferir datos hacia y desde el controller sin exponer la entidad completa.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioDto {

    private UUID id;

    private OffsetDateTime fechaRegistro;

    private String status;

    private Integer intentosActivacion;

    private OffsetDateTime ultimoIntento;

    private String motivoFallo;
}
