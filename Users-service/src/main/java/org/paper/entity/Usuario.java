package org.paper.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    private UUID id;

    @Column(name = "fecha_registro", nullable = false)
    private OffsetDateTime fechaRegistro;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UsuarioStatus status;

    // RELACIÓN: Un cliente tiene un diseñador asignado
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "disenador_id")
    @ToString.Exclude // Evitar recursión infinita en logs
    private Usuario disenador;

    // RELACIÓN INVERSA: Un diseñador tiene muchos clientes asignados
    // (Esta lista solo se llenará si el usuario actual actúa como diseñador)
    @OneToMany(mappedBy = "disenador", fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Usuario> clientesAsignados = new ArrayList<>();
}