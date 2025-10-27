package org.paper.dtoResponse;

import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO simplificado para listados de diseños (sin base64 para optimizar)
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DisenoSimpleDto {
    private Integer id;
    private String nombre;
    private String descripcion;
    private String status;
    private String plantillaNombre;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
}