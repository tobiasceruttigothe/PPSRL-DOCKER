package org.paper.dtoCreate;
import jakarta.persistence.Column;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlantillaCreateDto {

    private String nombre;
    private Integer materialId; // buscar el material a la hora de mapearlo
    private Integer tipoBolsaId; // buscar el tipoBolsa a la hora de mapearlo
    private String base64Plantilla;
    private Float ancho;
    private Float alto;
    private  Float profundidad;
}
