package org.paper.clients;

import lombok.extern.slf4j.Slf4j;
import org.paper.dto.UsuarioCreateDTO;
import org.paper.exception.KeycloakException;
import org.paper.exception.ValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;

/**
 * Cliente para todas las operaciones con Keycloak Admin API.
 * Centraliza toda la comunicación con Keycloak.
 */
@Slf4j
@Component
public class KeycloakClient {

    private final WebClient webClient;
    private final String realm;

    public KeycloakClient(WebClient webClient, @Value("${keycloak.realm}") String realm) {
        this.webClient = webClient;
        this.realm = realm;
    }

    // ==================== GESTIÓN DE USUARIOS ====================

    /**
     * Obtiene el ID de usuario de Keycloak por username
     */
    public String obtenerUserId(String username, String token) {
        try {
            List<Map<String, Object>> users = webClient.get()
                    .uri("/admin/realms/{realm}/users?username={username}&exact=true", realm, username)
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

    /**
     * Obtiene información completa de un usuario por ID
     */
    public Map<String, Object> obtenerUsuarioPorId(String userId, String token) {
        try {
            return webClient.get()
                    .uri("/admin/realms/{realm}/users/{id}", realm, userId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
        } catch (WebClientResponseException e) {
            log.error("Error obteniendo usuario por ID {}: {}", userId, e.getMessage());
            throw new KeycloakException("obtener usuario por ID", e.getStatusCode().value(), e.getResponseBodyAsString());
        }
    }

    /**
     * Busca usuario por email
     */
    public Map<String, Object> buscarUsuarioPorEmail(String email, String token) {
        try {
            List<Map<String, Object>> users = webClient.get()
                    .uri("/admin/realms/{realm}/users?email={email}&exact=true", realm, email)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToFlux(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .collectList()
                    .block();

            if (users == null || users.isEmpty()) {
                return null;
            }
            return users.get(0);
        } catch (WebClientResponseException e) {
            log.error("Error buscando usuario por email: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Crea un nuevo usuario en Keycloak
     */
    public void crearUsuario(UsuarioCreateDTO usuario, String token) {
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

            log.info("Usuario {} creado en Keycloak", usuario.getUsername());
        } catch (WebClientResponseException e) {
            throw new KeycloakException("crear usuario", e.getStatusCode().value(), e.getResponseBodyAsString());
        }
    }

    /**
     * Lista todos los usuarios del realm
     */
    public List<Map<String, Object>> listarUsuarios(String token) {
        return webClient.get()
                .uri("/admin/realms/{realm}/users", realm)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<Map<String, Object>>() {})
                .collectList()
                .block();
    }

    /**
     * Elimina un usuario de Keycloak
     */
    public void eliminarUsuario(String userId, String token) {
        try {
            webClient.delete()
                    .uri("/admin/realms/{realm}/users/{id}", realm, userId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            log.info("Usuario {} eliminado de Keycloak", userId);
        } catch (WebClientResponseException e) {
            throw new KeycloakException("eliminar usuario", e.getStatusCode().value(), "No se pudo eliminar el usuario de Keycloak");
        }
    }

    // ==================== GESTIÓN DE CONTRASEÑAS ====================

    /**
     * Asigna una contraseña permanente (no temporal)
     */
    public void asignarPassword(String userId, String password, String token) {
        cambiarPassword(userId, password, false, token);
    }

    /**
     * Asigna una contraseña temporal que debe cambiarse en el primer login
     */
    public void asignarPasswordTemporal(String userId, String password, String token) {
        cambiarPassword(userId, password, true, token);
    }

    /**
     * Cambia la contraseña de un usuario (método genérico)
     */
    public void cambiarPassword(String userId, String password, boolean temporal, String token) {
        String passwordJson = String.format(
                "{\"type\":\"password\",\"value\":\"%s\",\"temporary\":%b}",
                password, temporal
        );

        try {
            webClient.put()
                    .uri("/admin/realms/{realm}/users/{id}/reset-password", realm, userId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(passwordJson)
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            log.info("Contraseña {} para usuario {}", temporal ? "temporal asignada" : "actualizada", userId);
        } catch (WebClientResponseException e) {
            throw new KeycloakException("cambiar password", e.getStatusCode().value(), "No se pudo cambiar la contraseña");
        }
    }

    // ==================== GESTIÓN DE ROLES ====================

    /**
     * Lista los roles de realm asignados a un usuario
     */
    public List<Map<String, Object>> listarRolesDeUsuario(String userId, String token) {
        return webClient.get()
                .uri("/admin/realms/{realm}/users/{userId}/role-mappings/realm", realm, userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<Map<String, Object>>() {})
                .collectList()
                .block();
    }

    /**
     * Obtiene información de un rol por nombre
     */
    public Map<String, Object> obtenerRolPorNombre(String roleName, String token) {
        return webClient.get()
                .uri("/admin/realms/{realm}/roles/{roleName}", realm, roleName)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();
    }

    /**
     * Agrega roles de realm a un usuario
     */
    public void agregarRolesAUsuario(String userId, List<Map<String, Object>> roles, String token) {
        webClient.post()
                .uri("/admin/realms/{realm}/users/{userId}/role-mappings/realm", realm, userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .bodyValue(roles)
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    /**
     * Elimina roles de realm de un usuario
     */
    public void eliminarRolesDeUsuario(String userId, List<Map<String, Object>> roles, String token) {
        webClient.method(HttpMethod.DELETE)
                .uri("/admin/realms/{realm}/users/{userId}/role-mappings/realm", realm, userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .bodyValue(roles)
                .retrieve()
                .toBodilessEntity()
                .block();
    }
}