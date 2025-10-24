package org.example.dto;

import lombok.*;
import org.example.entity.DisenoStatus; // << IMPORTANTE
import java.time.LocalDateTime;
import java.util.UUID;

@Data @NoArgsConstructor @AllArgsConstructor
public class DisenoDto {
    private Integer id;
    private UUID usuarioId;
    private Integer plantillaId;
    private Integer imagenId;
    private String nombre;
    private String descripcion;
    private DisenoStatus status; // << CAMBIO: era String
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
}
