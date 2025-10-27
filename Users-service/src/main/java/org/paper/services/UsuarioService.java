package org.paper.services;

import lombok.extern.slf4j.Slf4j;
import org.paper.clients.KeycloakClient;
import org.paper.dto.UsuarioCreateDTO;
import org.paper.dto.UsuarioResponseDTO;
import org.paper.entity.Usuario;
import org.paper.entity.UsuarioStatus;
import org.paper.exception.KeycloakException;
import org.paper.exception.UsuarioNotFoundException;
import org.paper.exception.UsuarioYaExisteException;
import org.paper.exception.ValidationException;
import org.paper.repository.UsuarioRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio de lógica de negocio para usuarios.
 * Coordina operaciones entre Keycloak y la base de datos.
 */
@Slf4j
@Service
public class UsuarioService {

    private final KeycloakAdminService keycloakAdminService;
    private final UsuarioRepository usuarioRepository;
    private final EmailVerificationService emailVerificationService;
    private final KeycloakClient keycloakClient;

    public UsuarioService(KeycloakAdminService keycloakAdminService,
                          UsuarioRepository usuarioRepository,
                          EmailVerificationService emailVerificationService,
                          KeycloakClient keycloakClient) {
        this.keycloakAdminService = keycloakAdminService;
        this.usuarioRepository = usuarioRepository;
        this.emailVerificationService = emailVerificationService;
        this.keycloakClient = keycloakClient;
    }

    /**
     * Crea un nuevo usuario en el sistema
     */
    @Transactional
    public ResponseEntity<String> crearUsuario(UsuarioCreateDTO usuario) {
        log.info("Iniciando creación de usuario: {} con rol: {}", usuario.getUsername(), usuario.getRol());
        String token = keycloakAdminService.getAdminToken();

        // Verificar que el usuario no exista
        String existingUserId = keycloakClient.obtenerUserId(usuario.getUsername(), token);
        if (existingUserId != null) {
            throw new UsuarioYaExisteException(usuario.getUsername());
        }

        // 1. Crear usuario en Keycloak
        keycloakClient.crearUsuario(usuario, token);

        String userId = keycloakClient.obtenerUserId(usuario.getUsername(), token);
        if (userId == null) {
            throw new KeycloakException("obtener UUID", "No se pudo obtener el identificador del usuario creado");
        }
        log.info("Usuario creado en Keycloak con ID: {}", userId);

        try {
            // 2. Asignar contraseña temporal
            keycloakClient.asignarPasswordTemporal(userId, usuario.getPassword(), token);

            // 3. Asignar el rol especificado
            cambiarRolUsuario(userId, usuario.getRol(), token);

            // 4. Guardar en la base de datos
            Usuario entity = new Usuario(UUID.fromString(userId), OffsetDateTime.now(), UsuarioStatus.PENDING);
            //faltaria ACTIVO cuando verifique el mail
            usuarioRepository.save(entity);
            log.info("Usuario {} guardado en BD con estado ACTIVE", usuario.getUsername());

            // 5. Enviar email de verificación
            try {
                emailVerificationService.createAndSendVerification(userId, usuario.getUsername(), usuario.getEmail());
                log.info("Email de verificación enviado a {}", usuario.getEmail());
            } catch (Exception emailEx) {
                log.warn("No se pudo enviar email de verificación a {}: {}. Usuario creado de todas formas.",
                        usuario.getEmail(), emailEx.getMessage());
            }

            return ResponseEntity.ok("Usuario creado correctamente. Se ha enviado un email de verificación con la contraseña temporal.");

        } catch (Exception e) {
            log.error("Error al configurar usuario {}: {}", userId, e.getMessage(), e);
            // Rollback: eliminar usuario de Keycloak
            try {
                keycloakClient.eliminarUsuario(userId, token);
                log.info("Rollback ejecutado: usuario eliminado de Keycloak");
            } catch (Exception rollbackEx) {
                log.error("Error en rollback: {}", rollbackEx.getMessage());
            }
            throw new KeycloakException("configurar usuario", "Error al configurar el usuario recién creado", e);
        }
    }

    /**
     * Elimina un usuario del sistema
     */
    @Transactional
    public ResponseEntity<String> eliminarUsuario(String username) {
        log.info("Iniciando proceso de eliminación para usuario: {}", username);
        String token = keycloakAdminService.getAdminToken();
        String userId = keycloakClient.obtenerUserId(username, token);

        if (userId == null) {
            throw new UsuarioNotFoundException(username);
        }

        Optional<Usuario> backup = usuarioRepository.findById(UUID.fromString(userId));
        usuarioRepository.deleteById(UUID.fromString(userId));

        try {
            keycloakClient.eliminarUsuario(userId, token);
        } catch (Exception e) {
            log.error("Error eliminando en Keycloak. Restaurando backup en DB.", e);
            backup.ifPresent(usuarioRepository::save);
            throw e;
        }

        return ResponseEntity.ok("Usuario eliminado correctamente");
    }

    /**
     * Cambia el rol de un usuario (obtiene token automáticamente)
     */
    public void cambiarRolUsuarioConToken(String userId, String nuevoRol) {
        String token = keycloakAdminService.getAdminToken();
        cambiarRolUsuario(userId, nuevoRol, token);
    }

    /**
     * Cambia el rol de un usuario (usa token proporcionado)
     */
    public void cambiarRolUsuario(String userId, String nuevoRol, String token) {
        log.debug("Cambiando rol para usuario {}: {}", userId, nuevoRol);

        // Validar que el rol sea válido
        if (!nuevoRol.matches("^(ADMIN|CLIENTE|DISEÑADOR)$")) {
            throw new ValidationException("rol", "El rol debe ser ADMIN, CLIENTE o DISEÑADOR");
        }

        // Eliminar roles actuales
        List<Map<String, Object>> rolesActuales = keycloakClient.listarRolesDeUsuario(userId, token);
        if (rolesActuales != null && !rolesActuales.isEmpty()) {
            keycloakClient.eliminarRolesDeUsuario(userId, rolesActuales, token);
        }

        // Asignar nuevo rol
        Map<String, Object> rol = keycloakClient.obtenerRolPorNombre(nuevoRol, token);
        if (rol == null) {
            throw new ValidationException("rol", "El rol '" + nuevoRol + "' no existe");
        }

        keycloakClient.agregarRolesAUsuario(userId, List.of(rol), token);
        log.info("Rol {} asignado correctamente al usuario {}", nuevoRol, userId);
    }

    /**
     * Lista todos los usuarios del sistema
     */
    public ResponseEntity<List<UsuarioResponseDTO>> listarUsuarios() {
        String token = keycloakAdminService.getAdminToken();
        List<Map<String, Object>> users = keycloakClient.listarUsuarios(token);

        if (users == null) {
            return ResponseEntity.ok(new ArrayList<>());
        }

        List<UsuarioResponseDTO> response = users.stream()
                .map(user -> mapearUsuarioResponse(user, token))
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Lista usuarios filtrados por rol
     */
    public ResponseEntity<List<UsuarioResponseDTO>> listarUsuariosPorRol(String rolBuscado) {
        ResponseEntity<List<UsuarioResponseDTO>> allUsersResponse = listarUsuarios();

        if (!allUsersResponse.getStatusCode().is2xxSuccessful() || allUsersResponse.getBody() == null) {
            throw new KeycloakException("listar usuarios por rol", "No se pudo obtener la lista de usuarios");
        }

        List<UsuarioResponseDTO> filtrados = allUsersResponse.getBody().stream()
                .filter(u -> u.getRoles() != null && u.getRoles().contains(rolBuscado))
                .collect(Collectors.toList());

        return ResponseEntity.ok(filtrados);
    }

    // ==================== MÉTODOS PRIVADOS ====================

    /**
     * Mapea un usuario de Keycloak a DTO de respuesta
     */
    private UsuarioResponseDTO mapearUsuarioResponse(Map<String, Object> user, String token) {
        String userId = (String) user.get("id");

        // Obtener roles
        List<Map<String, Object>> rolesMap = keycloakClient.listarRolesDeUsuario(userId, token);
        List<String> roles = rolesMap.stream()
                .map(role -> (String) role.get("name"))
                .collect(Collectors.toList());

        // Obtener razón social de attributes
        String razonSocial = "";
        Map<String, Object> attributes = (Map<String, Object>) user.get("attributes");
        if (attributes != null && attributes.get("razonSocial") instanceof List) {
            List<String> rsList = (List<String>) attributes.get("razonSocial");
            if (!rsList.isEmpty()) {
                razonSocial = rsList.get(0);
            }
        }

        return new UsuarioResponseDTO(
                userId,
                (String) user.get("username"),
                (String) user.get("email"),
                razonSocial,
                roles
        );
    }
}