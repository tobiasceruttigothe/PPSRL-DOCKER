package org.example.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "plantillas")
public class Plantilla {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @ManyToOne(optional = false) @JoinColumn(name = "material_id")
    private Material material;

    @ManyToOne(optional = false) @JoinColumn(name = "tipo_bolsa_id")
    private TipoBolsa tipoBolsa;

    @ManyToOne(optional = false) @JoinColumn(name = "dimension_id")
    private Dimension dimension;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();
}
