package org.example.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "usuarios_plantillas")
public class UsuarioPlantilla {

    @EmbeddedId
    private UsuarioPlantillaId id;

    @MapsId("plantillaId")
    @ManyToOne(optional = false)
    @JoinColumn(name = "plantilla_id")
    private Plantilla plantilla;

    @Column(name = "fecha_habilitacion", nullable = false)
    private LocalDateTime fechaHabilitacion = LocalDateTime.now();
}
