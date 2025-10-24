package org.example.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @NoArgsConstructor @AllArgsConstructor
public class UsuarioPlantillaDto {
    private UUID usuarioId;
    private Integer plantillaId;
    private LocalDateTime fechaHabilitacion;
}
