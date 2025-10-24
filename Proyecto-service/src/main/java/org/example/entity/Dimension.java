package org.example.entity;

import jakarta.persistence.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "dimensiones")
public class Dimension {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ancho_mm", nullable = false)
    private Integer anchoMm;

    @Column(name = "alto_mm", nullable = false)
    private Integer altoMm;
}
