package org.example.entity;

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

    // Usuario viene de usuario-service: no mapeamos @ManyToOne
    @Column(name = "usuario_id", nullable = false, columnDefinition = "uuid")
    private UUID usuarioId;

    @ManyToOne(optional = false) @JoinColumn(name = "plantilla_id")
    private Plantilla plantilla;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @OneToOne(optional = false) @JoinColumn(name = "imagen_id", unique = true)
    private Imagen imagen;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DisenoStatus status = DisenoStatus.ACTIVO;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;


}
