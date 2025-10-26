package org.paper.dtoResponse;

import lombok.*;

/**
 * DTO simplificado para listados de plantillas (sin base64 para optimizar)
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PlantillaSimpleDto {
    private Integer id;
    private String nombre;
    private String materialNombre;
    private String tipoBolsaNombre;
    private Float ancho;
    private Float alto;
    private Float profundidad;
}