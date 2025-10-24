package org.example.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor
public class ImagenDto {
    private Integer id;
    private String formato;
    private Long tamanoBytes;
    private LocalDateTime fechaCreacion;
    private String base64Data; // si usas texto base64
}
