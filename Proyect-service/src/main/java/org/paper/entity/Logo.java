package org.paper.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "logos")
public class Logo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id", columnDefinition = "uuid")
    private Usuario usuario;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Lob
    @Column(name = "base64_logo",nullable = false)
    private String base64Logo;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "tamano_bytes")
    private Long tamanoBytes;
}
