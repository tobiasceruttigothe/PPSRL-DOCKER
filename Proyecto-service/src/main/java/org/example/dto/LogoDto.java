package org.example.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogoDto {
    private Integer id;
    private UUID usuarioId;
    private String nombre;
    private Long tamanoBytes;
    private LocalDateTime fechaCreacion;
    private String base64Logo;
}
