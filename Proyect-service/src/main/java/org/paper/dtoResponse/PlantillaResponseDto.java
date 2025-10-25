package org.paper.dtoResponse;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.paper.entity.Material;
import org.paper.entity.TipoBolsa;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlantillaResponseDto {

    private Integer id;
    private String nombre;
    private String base64Diseno;
    private Material material;
    private TipoBolsa tipoBolsa;
    private Float ancho;
    private Float alto;
    private Float profundidad;

}
