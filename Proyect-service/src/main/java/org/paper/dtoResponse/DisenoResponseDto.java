package org.paper.dtoResponse;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class DisenoResponseDto {

    private Integer id;
    private String nombre;
    private String descripcion;
    private String status;
    private String base64Diseno;
}
