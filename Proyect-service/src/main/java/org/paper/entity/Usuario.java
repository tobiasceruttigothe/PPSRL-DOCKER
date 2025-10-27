package org.paper.entity;


import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "usuarios")
public class Usuario {
    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "fecha_registro", nullable = false)
    private OffsetDateTime fechaRegistro;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado",nullable = false, length = 20)
    private UsuarioStatus status;

   // @Column(length = 20, nullable = false)
    //private String status;

    // Relaciones
    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Logo> logos = new ArrayList<>();

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Diseno> disenos = new ArrayList<>();

    // Plantillas habilitadas para el usuario
    @ManyToMany
    @JoinTable(
            name = "usuario_plantilla",
            joinColumns = @JoinColumn(name = "usuario_id", columnDefinition = "uuid"),
            inverseJoinColumns = @JoinColumn(name = "plantilla_id")
    )
    private Set<Plantilla> plantillasHabilitadas = new HashSet<>();
}
