package org.paper.services;

import lombok.extern.slf4j.Slf4j;
import org.paper.dto.UsuarioCreateDTO;
import org.paper.dto.UsuarioResponseDTO;
import org.paper.entity.Usuario;
import org.paper.entity.UsuarioStatus;
import org.paper.exception.KeycloakException;
import org.paper.exception.UsuarioNotFoundException;
import org.paper.exception.UsuarioYaExisteException;
import org.paper.exception.ValidationException;
import org.paper.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UsuarioService {

    private final KeycloakAdminService keycloakAdminService;
    private final UsuarioRepository usuarioRepository;
    private final EmailVerificationService emailVerificationService;
    private final WebClient webClient;

    @Value("${keycloak.realm}")
    private String realm;

    public UsuarioService(KeycloakAdminService keycloakAdminService,
                          UsuarioRepository usuarioRepository,
                          EmailVerificationService emailVerificationService,
                          WebClient webClient) {
        this.keycloakAdminService = keycloakAdminService;
        this.usuarioRepository = usuarioRepository;
        this.emailVerificationService = emailVerificationService;
        this.webClient = webClient;
    }

    @Transactional
    public ResponseEntity<String> crearUsuario(UsuarioCreateDTO usuario) {
        log.info("Iniciando creación de usuario: {} con rol: {}", usuario.getUsername(), usuario.getRol());
        String token = keycloakAdminService.getAdminToken();

        // Verificar que el usuario no exista
        String existingUserId = obtenerUserId(usuario.getUsername(), token);
        if (existingUserId != null) {
            throw new UsuarioYaExisteException(usuario.getUsername());
        }

        // 1. Crear usuario en Keycloak
        crearUsuarioEnKeycloak(usuario, token);

        String userId = obtenerUserId(usuario.getUsername(), token);
        if (userId == null) {
            throw new KeycloakException("obtener UUID", "No se pudo obtener el identificador del usuario creado");
        }
        log.info("Usuario creado en Keycloak con ID: {}", userId);

        try {
            // 2. Asignar contraseña temporal (requiere cambio en primer login)
            asignarPasswordTemporal(userId, usuario.getPassword(), token);
            log.info("Contraseña temporal asignada para usuario: {}", userId);

            // 3. Asignar el rol especificado
            cambiarRolUsuario(userId, usuario.getRol(), token);
            log.info("Rol {} asignado correctamente al usuario {}", usuario.getRol(), userId);

            // 4. Guardar en la base de datos con estado ACTIVE
            Usuario entity = new Usuario(UUID.fromString(userId));
            entity.setStatus(UsuarioStatus.ACTIVE);
            entity.setIntentosActivacion(0);
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
            // Rollback: eliminar usuario de Keycloak si falló la configuración
            try {
                eliminarUsuarioDeKeycloak(userId, token);
                log.info("Rollback ejecutado: usuario eliminado de Keycloak");
            } catch (Exception rollbackEx) {
                log.error("Error en rollback: {}", rollbackEx.getMessage());
            }
            throw new KeycloakException("configurar usuario", "Error al configurar el usuario recién creado", e);
        }
    }

    @Transactional
    public ResponseEntity<String> eliminarUsuario(String username) {
        log.info("Iniciando proceso de eliminación para usuario: {}", username);
        String token = keycloakAdminService.getAdminToken();
        String userId = obtenerUserId(username, token);

        if (userId == null) {
            throw new UsuarioNotFoundException(username);
        }

        Optional<Usuario> backup = usuarioRepository.findById(UUID.fromString(userId));
        usuarioRepository.deleteById(UUID.fromString(userId));

        try {
            eliminarUsuarioDeKeycloak(userId, token);
        } catch (Exception e) {
            log.error("Error eliminando en Keycloak. Restaurando backup en DB.", e);
            backup.ifPresent(usuarioRepository::save);
            throw e;
        }

        return ResponseEntity.ok("Usuario eliminado correctamente");
    }

    public void cambiarRolUsuarioConToken(String userId, String nuevoRol) {
        String token = keycloakAdminService.getAdminToken();
        cambiarRolUsuario(userId, nuevoRol, token);
    }

    public void cambiarRolUsuario(String userId, String nuevoRol, String token) {
        log.debug("Cambiando rol para usuario {}: {}", userId, nuevoRol);

        // Validar que el rol sea válido
        if (!nuevoRol.matches("^(ADMIN|CLIENTE|DISEÑADOR)$")) {
            throw new ValidationException("rol", "El rol debe ser ADMIN, CLIENTE o DISEÑADOR");
        }

        List<Map<String, Object>> rolesActuales = listarRolesDeUsuario(userId, token);

        if (rolesActuales != null && !rolesActuales.isEmpty()) {
            eliminarRolesDeUsuario(userId, rolesActuales, token);
        }

        Map<String, Object> rol = obtenerRolPorNombre(nuevoRol, token);
        if (rol == null) {
            throw new ValidationException("rol", "El rol '" + nuevoRol + "' no existe");
        }

        agregarRolesAUsuario(userId, List.of(rol), token);
        log.info("Rol {} asignado correctamente al usuario {}", nuevoRol, userId);
    }

    public ResponseEntity<List<UsuarioResponseDTO>> listarUsuarios() {
        String token = keycloakAdminService.getAdminToken();
        List<Map<String, Object>> users = listarUsuariosDeKeycloak(token);

        if (users == null) {
            return ResponseEntity.ok(new ArrayList<>());
        }

        List<UsuarioResponseDTO> response = users.stream().map(user -> {
            String userId = (String) user.get("id");
            List<Map<String, Object>> rolesMap = listarRolesDeUsuario(userId, token);
            List<String> roles = rolesMap.stream()
                    .map(role -> (String) role.get("name"))
                    .collect(Collectors.toList());

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
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

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

    // ==================== MÉTODOS PRIVADOS PARA KEYCLOAK ====================

    private String obtenerUserId(String username, String token) {
        try {
            List<Map<String, Object>> users = webClient.get()
                    .uri("/admin/realms/{realm}/users?username={username}", realm, username)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToFlux(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .collectList()
                    .block();

            if (users == null || users.isEmpty()) {
                return null;
            }
            if (users.size() > 1) {
                throw new ValidationException("username", "Múltiples usuarios encontrados con el mismo nombre");
            }
            return (String) users.get(0).get("id");
        } catch (WebClientResponseException e) {
            throw new KeycloakException("obtener usuario", e.getStatusCode().value(), e.getResponseBodyAsString());
        }
    }

    private void crearUsuarioEnKeycloak(UsuarioCreateDTO usuario, String token) {
        String jsonBody = String.format(
                "{ \"username\":\"%s\", \"enabled\":%b, \"email\":\"%s\", \"emailVerified\":%b, \"attributes\": { \"razonSocial\": [\"%s\"] } }",
                usuario.getUsername(), usuario.isEnabled(), usuario.getEmail(), usuario.isEmailVerified(), usuario.getRazonSocial()
        );
        try {
            webClient.post()
                    .uri("/admin/realms/{realm}/users", realm)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(jsonBody)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (WebClientResponseException e) {
            throw new KeycloakException("crear usuario", e.getStatusCode().value(), e.getResponseBodyAsString());
        }
    }

    private void asignarPasswordTemporal(String userId, String password, String token) {
        String passwordJson = String.format("{\"type\":\"password\",\"value\":\"%s\",\"temporary\":true}", password);
        try {
            webClient.put()
                    .uri("/admin/realms/{realm}/users/{id}/reset-password", realm, userId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(passwordJson)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            log.info("Contraseña temporal asignada para usuario: {}", userId);
        } catch (WebClientResponseException e) {
            throw new KeycloakException("asignar password temporal", e.getStatusCode().value(),
                    "No se pudo asignar la contraseña temporal");
        }
    }

    private void eliminarUsuarioDeKeycloak(String userId, String token) {
        try {
            webClient.delete()
                    .uri("/admin/realms/{realm}/users/{id}", realm, userId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (WebClientResponseException e) {
            throw new KeycloakException("eliminar usuario", e.getStatusCode().value(),
                    "No se pudo eliminar el usuario de Keycloak");
        }
    }

    private List<Map<String, Object>> listarUsuariosDeKeycloak(String token) {
        return webClient.get()
                .uri("/admin/realms/{realm}/users", realm)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<Map<String, Object>>() {})
                .collectList()
                .block();
    }

    private List<Map<String, Object>> listarRolesDeUsuario(String userId, String token) {
        return webClient.get()
                .uri("/admin/realms/{realm}/users/{userId}/role-mappings/realm", realm, userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<Map<String, Object>>() {})
                .collectList()
                .block();
    }

    private void eliminarRolesDeUsuario(String userId, List<Map<String, Object>> roles, String token) {
        webClient.method(HttpMethod.DELETE)
                .uri("/admin/realms/{realm}/users/{userId}/role-mappings/realm", realm, userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .bodyValue(roles)
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    private Map<String, Object> obtenerRolPorNombre(String roleName, String token) {
        return webClient.get()
                .uri("/admin/realms/{realm}/roles/{roleName}", realm, roleName)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();
    }

    private void agregarRolesAUsuario(String userId, List<Map<String, Object>> roles, String token) {
        webClient.post()
                .uri("/admin/realms/{realm}/users/{userId}/role-mappings/realm", realm, userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .bodyValue(roles)
                .retrieve()
                .toBodilessEntity()
                .block();
    }
}