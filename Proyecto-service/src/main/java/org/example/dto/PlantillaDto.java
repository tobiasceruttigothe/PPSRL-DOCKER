package org.example.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor
public class PlantillaDto {
    private Integer id;
    private String nombre;
    private Integer materialId;
    private Integer tipoBolsaId;
    private Integer dimensionId;
    private LocalDateTime fechaCreacion;
}
