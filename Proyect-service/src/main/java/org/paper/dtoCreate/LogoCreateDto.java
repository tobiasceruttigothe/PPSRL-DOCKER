package org.paper.dtoCreate;

import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogoCreateDto {
    private UUID usuarioId; //buscar el usuario a la hora de mapearlo
    private String nombre;
    private String base64Logo;
}