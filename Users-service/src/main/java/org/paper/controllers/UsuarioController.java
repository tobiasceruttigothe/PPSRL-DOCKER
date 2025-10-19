package org.paper.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.paper.dto.ErrorResponse;
import org.paper.dto.UsuarioCreateDTO;
import org.paper.dto.UsuarioResponseDTO;
import org.paper.repository.UsuarioRepository;
import org.paper.services.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/usuarios")
@Tag(name = "Usuarios", description = "Gestión de usuarios del sistema")
@SecurityRequirement(name = "Bearer Authentication")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;

    public UsuarioController(UsuarioService usuarioService,
                             UsuarioRepository usuarioRepository) {
        this.usuarioService = usuarioService;
        this.usuarioRepository = usuarioRepository;
    }

    @PostMapping("/create")
    @Operation(
            summary = "Crear nuevo usuario (solo ADMIN)",
            description = """
            Crea un nuevo usuario en el sistema. Solo los administradores pueden crear usuarios.
            
            **Flujo:**
            1. Admin crea usuario con rol específico y contraseña temporal
            2. Usuario recibe email de verificación
            3. Usuario verifica email y hace login
            4. Keycloak detecta la contraseña temporal y fuerza el cambio
            5. Usuario establece su contraseña definitiva
            
            **Roles disponibles:** ADMIN, CLIENTE, DISEÑADOR
            """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Usuario creado correctamente",
                    content = @Content(mediaType = "text/plain")
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos de entrada inválidos (validación falló)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado (token inválido o expirado)"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Sin permisos (solo ADMIN puede crear usuarios)"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "El usuario ya existe en el sistema",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "502",
                    description = "Error de comunicación con Keycloak",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<String> crearUsuario(
            @Valid @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = """
                    Datos del nuevo usuario. Todos los campos son obligatorios.
                    - **rol**: debe ser ADMIN, CLIENTE o DISEÑADOR
                    - **password**: contraseña temporal (usuario deberá cambiarla en primer login)
                    """,
                    required = true
            )
            UsuarioCreateDTO usuarioDTO) {
        return usuarioService.crearUsuario(usuarioDTO);
    }

    @PutMapping("/{userId}/rol/admin")
    @Operation(
            summary = "Asignar rol de administrador",
            description = """
            Cambia el rol de un usuario a ADMIN. 
            
            **Requiere:** Rol ADMIN
            
            ⚠️ **Nota:** El userId es el UUID de Keycloak, no el username.
            """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rol cambiado correctamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado (token inválido o expirado)"),
            @ApiResponse(responseCode = "403", description = "Sin permisos (rol insuficiente)"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    public ResponseEntity<String> asignarRolAdmin(
            @Parameter(
                    description = "ID del usuario (UUID de Keycloak)",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable String userId) {
        usuarioService.cambiarRolUsuarioConToken(userId, "ADMIN");
        return ResponseEntity.ok("Rol cambiado a ADMIN");
    }

    @PutMapping("/{userId}/rol/cliente")
    @Operation(
            summary = "Asignar rol de cliente",
            description = """
            Cambia el rol de un usuario a CLIENTE.
            
            **Requiere:** Rol ADMIN
            
            ⚠️ **Nota:** El userId es el UUID de Keycloak, no el username.
            """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rol cambiado correctamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    public ResponseEntity<String> asignarRolCliente(
            @Parameter(
                    description = "ID del usuario (UUID de Keycloak)",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable String userId) {
        usuarioService.cambiarRolUsuarioConToken(userId, "CLIENTE");
        return ResponseEntity.ok("Rol cambiado a CLIENTE");
    }

    @PutMapping("/{userId}/rol/disenador")
    @Operation(
            summary = "Asignar rol de diseñador",
            description = """
            Cambia el rol de un usuario a DISEÑADOR.
            
            **Requiere:** Rol ADMIN
            
            ⚠️ **Nota:** El userId es el UUID de Keycloak, no el username.
            """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rol cambiado correctamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    public ResponseEntity<String> asignarRolDisenador(
            @Parameter(
                    description = "ID del usuario (UUID de Keycloak)",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable String userId) {
        usuarioService.cambiarRolUsuarioConToken(userId, "DISEÑADOR");
        return ResponseEntity.ok("Rol cambiado a DISEÑADOR");
    }

    @DeleteMapping("/eliminate/{username}")
    @Operation(
            summary = "Eliminar usuario",
            description = """
            Elimina un usuario del sistema (tanto de Keycloak como de la base de datos).
            
            **Requiere:** Rol ADMIN
            
            ⚠️ **Precaución:** Esta acción es irreversible.
            """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario eliminado correctamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
            @ApiResponse(responseCode = "500", description = "Error al eliminar (rollback aplicado)")
    })
    public ResponseEntity<String> eliminarUsuario(
            @Parameter(
                    description = "Nombre de usuario (username)",
                    required = true,
                    example = "juan_perez"
            )
            @PathVariable String username) {
        return usuarioService.eliminarUsuario(username);
    }

    @GetMapping("/list/users")
    @Operation(
            summary = "Listar todos los usuarios",
            description = """
            Retorna la lista completa de usuarios del sistema con sus roles y datos básicos.
            
            **Requiere:** Rol ADMIN
            """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de usuarios obtenida correctamente"
            ),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos")
    })
    public ResponseEntity<List<UsuarioResponseDTO>> listarUsuarios() {
        return usuarioService.listarUsuarios();
    }

    @GetMapping("/list/users/disenadores")
    @Operation(
            summary = "Listar diseñadores",
            description = """
            Retorna usuarios con rol DISEÑADOR.
            
            **Requiere:** Rol ADMIN
            """
    )
    public ResponseEntity<List<UsuarioResponseDTO>> listarUsuariosDisenadores() {
        return usuarioService.listarUsuariosPorRol("DISEÑADOR");
    }

    @GetMapping("/list/users/clients")
    @Operation(
            summary = "Listar usuarios clientes",
            description = """
            Retorna usuarios con rol CLIENTE (usuarios activos del sistema).
            
            **Requiere:** Rol ADMIN
            """
    )
    public ResponseEntity<List<UsuarioResponseDTO>> listarUsuariosClientes() {
        return usuarioService.listarUsuariosPorRol("CLIENTE");
    }

    @GetMapping("/list/users/admins")
    @Operation(
            summary = "Listar administradores",
            description = """
            Retorna usuarios con rol ADMIN.
            
            **Requiere:** Rol ADMIN
            """
    )
    public ResponseEntity<List<UsuarioResponseDTO>> listarUsuariosAdmins() {
        return usuarioService.listarUsuariosPorRol("ADMIN");
    }
}