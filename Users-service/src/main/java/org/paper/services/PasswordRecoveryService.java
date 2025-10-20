package org.paper.services;

import lombok.extern.slf4j.Slf4j;
import org.paper.clients.KeycloakClient;
import org.paper.exception.UsuarioNotFoundException;
import org.paper.security.JwtUtil;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Servicio para recuperación y cambio de contraseñas.
 * Maneja flujos de "olvidé mi contraseña" y cambio de password temporal.
 */
@Service
@Slf4j
public class PasswordRecoveryService {

    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final KeycloakAdminService keycloakAdminService;
    private final KeycloakClient keycloakClient;

    public PasswordRecoveryService(JwtUtil jwtUtil,
                                   EmailService emailService,
                                   KeycloakAdminService keycloakAdminService,
                                   KeycloakClient keycloakClient) {
        this.jwtUtil = jwtUtil;
        this.emailService = emailService;
        this.keycloakAdminService = keycloakAdminService;
        this.keycloakClient = keycloakClient;
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
            Map<String, Object> user = keycloakClient.buscarUsuarioPorEmail(email, token);

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
            keycloakClient.cambiarPassword(userId, newPassword, false, adminToken);

            // Obtener username para el email
            Map<String, Object> user = keycloakClient.obtenerUsuarioPorId(userId, adminToken);
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
            // Verificar que el usuario exista
            Map<String, Object> user = keycloakClient.obtenerUsuarioPorId(userId, adminToken);
            if (user == null) {
                throw new UsuarioNotFoundException(userId);
            }

            // Cambiar password en Keycloak (NO temporal)
            keycloakClient.cambiarPassword(userId, newPassword, false, adminToken);

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
}