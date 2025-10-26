package org.paper.dtoResponse;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogoResponseDto {
    private Integer id;
    private String nombre;
    private String base64Logo;
}