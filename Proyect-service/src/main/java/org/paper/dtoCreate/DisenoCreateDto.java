package org.paper.dtoCreate;

import lombok.*;
import org.paper.entity.DisenoStatus;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DisenoCreateDto {

    private UUID usuarioId; // buscar el usuario a la hora de mapearlo
    private Integer plantillaId; // buscar la plantilla a la hora de mapearlo
    private String nombre;
    private String descripcion;
    private String base64Diseno;
}
