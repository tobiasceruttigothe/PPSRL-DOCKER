package org.example.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Data @NoArgsConstructor @AllArgsConstructor
@Embeddable
public class UsuarioPlantillaId implements Serializable {
    @Column(name = "usuario_id", columnDefinition = "uuid")
    private UUID usuarioId;

    @Column(name = "plantilla_id")
    private Integer plantillaId;
}
