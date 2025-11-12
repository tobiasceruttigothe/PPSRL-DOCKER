package org.paper.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "disenos")
public class Diseno {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id", columnDefinition = "uuid")
    private Usuario usuario;


    @ManyToOne(optional = false)
    @JoinColumn(name = "plantilla_id")
    private Plantilla plantilla;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Lob
    @Column(name = "base64_diseno", nullable = false, columnDefinition = "text")
    private String base64Diseno;

    @Lob
    @Column(name = "base64_preview", nullable = false, columnDefinition = "text")
    private String base64Preview;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado",nullable = false, length = 20)
    private DisenoStatus status = DisenoStatus.PROGRESO;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;
}
