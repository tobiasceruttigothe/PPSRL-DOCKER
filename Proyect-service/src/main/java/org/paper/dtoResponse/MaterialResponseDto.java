package org.paper.dtoResponse;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaterialResponseDto {
    private Integer id;
    private String nombre;
}