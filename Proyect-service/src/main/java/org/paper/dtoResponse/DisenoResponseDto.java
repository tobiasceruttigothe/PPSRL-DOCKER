package org.paper.dtoResponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class DisenoResponseDto {
    private Integer id;
    private String nombre;
    private String descripcion;
    private String status;
    private String base64Diseno;
    private String base64Preview;
    private Integer plantillaId;
    private String plantillaNombre;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
}