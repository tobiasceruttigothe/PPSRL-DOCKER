package org.paper.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = "usuariosHabilitados")
@Entity @Table(name = "plantillas")
public class Plantilla {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @ManyToOne(optional = false) @JoinColumn(name = "material_id")
    private Material material;

    @ManyToOne(optional = false) @JoinColumn(name = "tipo_bolsa_id")
    private TipoBolsa tipoBolsa;

    @Lob
    @Column(name = "base64_plantilla", nullable = false)
    private String base64Plantilla;

    @Column(name = "ancho",nullable = false)
    private Float ancho;

    @Column(name = "alto", nullable = false)
    private Float alto;

    @Column(name = "profundidad",nullable = false)
    private  Float profundidad;

    @ManyToMany(mappedBy = "plantillasHabilitadas")
    private Set<Usuario> usuariosHabilitados = new HashSet<>();
}
