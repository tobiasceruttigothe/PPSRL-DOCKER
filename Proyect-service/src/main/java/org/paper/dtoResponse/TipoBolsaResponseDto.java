package org.paper.dtoResponse;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TipoBolsaResponseDto {
    private Integer id;
    private String nombre;
}