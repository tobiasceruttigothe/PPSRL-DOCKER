package org.example.entity;// Usuarios.java


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

    @Column(length = 20, nullable = false)
    private String status;

    @Column(name = "intentos_activacion", nullable = false)
    private Integer intentosActivacion = 0;

    @Column(name = "ultimo_intento")
    private OffsetDateTime ultimoIntento;

    @Column(name = "motivo_fallo")
    private String motivoFallo;

    // Relaciones
    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Logo> logos = new ArrayList<>();

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Diseno> disenos = new ArrayList<>();

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UsuarioPlantilla> habilitaciones = new HashSet<>();

    // Lombok genera getters/setters/constructores.
}
