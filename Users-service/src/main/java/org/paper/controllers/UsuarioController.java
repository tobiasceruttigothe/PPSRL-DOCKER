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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

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


    @GetMapping("/clientes-asignados") // Renombrado para ser más semántico (ya no son siempre "mis")
    @Operation(
            summary = "Listar clientes asignados a un diseñador",
            description = """
            Retorna los clientes asignados a un diseñador específico.
            
            **Reglas de acceso:**
            - **DISEÑADOR:** Por defecto retorna *sus propios* clientes. Si envía un ID distinto al suyo, se bloquea (403).
            - **ADMIN:** Debe enviar el parámetro `disenadorId` para especificar qué lista quiere ver.
            """
    )
    public ResponseEntity<List<UsuarioResponseDTO>> listarClientesAsignados(
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) String requesterId,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Roles", required = false) String roles,
            @Parameter(description = "ID del diseñador a consultar (Obligatorio para ADMIN, opcional para DISEÑADOR)")
            @RequestParam(required = false) String disenadorId) {

        // 1. Validar roles básicos
        boolean isAdmin = roles != null && roles.contains("ADMIN");
        boolean isDisenador = roles != null && roles.contains("DISEÑADOR");

        if (!isAdmin && !isDisenador) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        String targetId;

        // 2. Lógica de selección del ID objetivo
        if (isAdmin) {
            // Si es ADMIN, es OBLIGATORIO que diga a quién quiere consultar
            if (disenadorId == null || disenadorId.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            targetId = disenadorId;
        } else {
            // Si es DISEÑADOR
            // Si intenta ver los de otro (manda param distinto a su header), 403 Forbidden
            if (disenadorId != null && !disenadorId.equals(requesterId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            // Si no manda nada o manda su propio ID, usamos su ID del token
            targetId = requesterId;
        }

        // Validación final por si acaso
        if (targetId == null) {
            return ResponseEntity.badRequest().build();
        }

        // 3. Consultar servicio
        List<UsuarioResponseDTO> clientes = usuarioService.listarMisClientes(targetId);
        return ResponseEntity.ok(clientes);
    }

    @PutMapping("/{clienteId}/asignar-disenador/{disenadorId}")
    @Operation(
            summary = "Asignar o cambiar diseñador",
            description = """
            Asigna un diseñador a un cliente existente. Si ya tenía uno, lo reemplaza.
            **Requiere rol ADMIN.**
            """
    )
    public ResponseEntity<String> asignarDisenador(
            @Parameter(description = "ID del Cliente") @PathVariable String clienteId,
            @Parameter(description = "ID del Diseñador") @PathVariable UUID disenadorId) {

        usuarioService.asignarODesaignarDisenador(clienteId, disenadorId);
        return ResponseEntity.ok("Diseñador asignado correctamente al cliente");
    }

    @DeleteMapping("/{clienteId}/asignar-disenador")
    @Operation(
            summary = "Desasignar diseñador",
            description = "Quita el diseñador asignado a un cliente. **Requiere rol ADMIN.**"
    )
    public ResponseEntity<String> desasignarDisenador(
            @PathVariable String clienteId) {

        usuarioService.asignarODesaignarDisenador(clienteId, null);
        return ResponseEntity.ok("Diseñador desasignado correctamente");
    }


}