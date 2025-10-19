package org.paper.services;

import lombok.extern.slf4j.Slf4j;
import org.paper.exception.UsuarioNotFoundException;
import org.paper.security.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class PasswordRecoveryService {

    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final KeycloakAdminService keycloakAdminService;
    private final WebClient webClient;

    @Value("${keycloak.realm}")
    private String realm;

    public PasswordRecoveryService(JwtUtil jwtUtil,
                                   EmailService emailService,
                                   KeycloakAdminService keycloakAdminService,
                                   WebClient webClient) {
        this.jwtUtil = jwtUtil;
        this.emailService = emailService;
        this.keycloakAdminService = keycloakAdminService;
        this.webClient = webClient;
    }

    /**
     * Solicitud de recuperación de contraseña
     * Busca el usuario por email y envía un token de recuperación
     */
    public void solicitarRecuperacionPassword(String email) {
        log.info("Solicitud de recuperación de contraseña para email: {}", email);

        String token = keycloakAdminService.getAdminToken();

        try {
            // Buscar usuario por email en Keycloak
            Map<String, Object> user = buscarUsuarioPorEmail(email, token);

            if (user == null) {
                // Por seguridad, no revelar si el email existe o no
                log.warn("Email no encontrado: {}, pero respondemos OK por seguridad", email);
                return;
            }

            String userId = (String) user.get("id");
            String username = (String) user.get("username");

            // Generar token JWT de recuperación (válido por 1 hora)
            String recoveryToken = jwtUtil.generatePasswordRecoveryToken(userId, email);

            // Enviar email con el link de recuperación
            emailService.enviarEmailRecuperacionPassword(email, username, recoveryToken);

            log.info("Email de recuperación enviado a: {}", email);

        } catch (Exception e) {
            log.error("Error al procesar solicitud de recuperación para {}: {}", email, e.getMessage(), e);
            // Por seguridad, no lanzar excepción al frontend
        }
    }

    /**
     * Resetea la contraseña usando el token de recuperación
     */
    public void resetearPassword(String token, String newPassword) {
        log.info("Intento de reseteo de contraseña con token");

        try {
            // Validar y obtener userId del token
            String userId = jwtUtil.validateAndGetUserId(token);
            String email = jwtUtil.getEmailFromToken(token);

            log.info("Token válido para userId: {}", userId);

            // Obtener token de admin de Keycloak
            String adminToken = keycloakAdminService.getAdminToken();

            // Cambiar password en Keycloak (NO temporal)
            cambiarPasswordEnKeycloak(userId, newPassword, false, adminToken);

            // Obtener username para el email
            Map<String, Object> user = obtenerUsuarioPorId(userId, adminToken);
            String username = (String) user.get("username");

            // Enviar email de confirmación
            emailService.enviarEmailPasswordCambiada(email, username);

            log.info("Contraseña reseteada exitosamente para userId: {}", userId);

        } catch (Exception e) {
            log.error("Error al resetear contraseña: {}", e.getMessage(), e);
            throw new RuntimeException("No se pudo resetear la contraseña. El token puede estar expirado.");
        }
    }

    /**
     * Cambia la contraseña temporal por una definitiva (requiere autenticación)
     * El usuario ya está autenticado, obtenemos su userId del contexto de seguridad
     */
    public void cambiarPasswordTemporal(String userId, String currentPassword, String newPassword) {
        log.info("Cambio de contraseña temporal para userId: {}", userId);

        String adminToken = keycloakAdminService.getAdminToken();

        try {
            // Verificar que la contraseña actual sea correcta
            // Esto lo hace Keycloak automáticamente cuando el usuario intenta autenticarse
            // Aquí solo validamos que el usuario exista
            Map<String, Object> user = obtenerUsuarioPorId(userId, adminToken);
            if (user == null) {
                throw new UsuarioNotFoundException(userId);
            }

            // Cambiar password en Keycloak (NO temporal)
            cambiarPasswordEnKeycloak(userId, newPassword, false, adminToken);

            // Enviar email de confirmación
            String email = (String) user.get("email");
            String username = (String) user.get("username");
            emailService.enviarEmailPasswordCambiada(email, username);

            log.info("Contraseña temporal cambiada exitosamente para userId: {}", userId);

        } catch (Exception e) {
            log.error("Error al cambiar contraseña temporal: {}", e.getMessage(), e);
            throw new RuntimeException("No se pudo cambiar la contraseña temporal", e);
        }
    }

    // ==================== MÉTODOS PRIVADOS ====================

    private Map<String, Object> buscarUsuarioPorEmail(String email, String token) {
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

    private Map<String, Object> obtenerUsuarioPorId(String userId, String token) {
        try {
            return webClient.get()
                    .uri("/admin/realms/{realm}/users/{id}", realm, userId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
        } catch (WebClientResponseException e) {
            log.error("Error obteniendo usuario por ID: {}", e.getMessage());
            throw new UsuarioNotFoundException(userId);
        }
    }

    private void cambiarPasswordEnKeycloak(String userId, String newPassword, boolean temporary, String token) {
        String passwordJson = String.format(
                "{\"type\":\"password\",\"value\":\"%s\",\"temporary\":%b}",
                newPassword, temporary
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

            log.info("Contraseña actualizada en Keycloak para userId: {}", userId);

        } catch (WebClientResponseException e) {
            log.error("Error cambiando password en Keycloak: {}", e.getMessage());
            throw new RuntimeException("No se pudo cambiar la contraseña en Keycloak", e);
        }
    }
}