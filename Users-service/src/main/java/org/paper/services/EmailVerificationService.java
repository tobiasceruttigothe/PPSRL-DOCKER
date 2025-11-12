package org.paper.services;

import lombok.extern.slf4j.Slf4j;
import org.paper.clients.KeycloakClient;
import org.paper.exception.ValidationException;
import org.paper.security.JwtUtil;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class EmailVerificationService {

    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final KeycloakAdminService keycloakAdminService;
    private final KeycloakClient keycloakClient;

    public EmailVerificationService(JwtUtil jwtUtil,
                                    EmailService emailService,
                                    KeycloakAdminService keycloakAdminService,
                                    KeycloakClient keycloakClient) {
        this.jwtUtil = jwtUtil;
        this.emailService = emailService;
        this.keycloakAdminService = keycloakAdminService;
        this.keycloakClient = keycloakClient;
    }

    /**
     * Genera token y envía email de activación de cuenta
     * (Se llama al crear un usuario nuevo)
     */
    public void createAndSendActivation(String userId, String username, String email) {
        String token = jwtUtil.generateVerificationToken(userId, email);
        emailService.enviarEmailActivacion(email, username, token);
        log.info("Token de activación generado y email enviado para userId {}", userId);
    }

    /**
     * NUEVO MÉTODO: Activa la cuenta del usuario en un solo paso
     * 1. Verifica el token
     * 2. Marca email como verificado
     * 3. Establece la contraseña definitiva
     * 4. Elimina la required action UPDATE_PASSWORD
     */
    public void activateAccount(String token, String newPassword) {
        try {
            // 1. Validar token y obtener userId
            String userId = jwtUtil.validateAndGetUserId(token);
            String email = jwtUtil.getEmailFromToken(token);

            log.info("Activando cuenta para userId: {}", userId);

            String adminToken = keycloakAdminService.getAdminToken();

            // 2. Verificar que el usuario existe
            Map<String, Object> user = keycloakClient.obtenerUsuarioPorId(userId, adminToken);
            if (user == null) {
                throw new ValidationException("Usuario no encontrado");
            }

            // 3. Verificar que el email no esté ya verificado (evitar reactivaciones)
            Boolean emailVerified = (Boolean) user.get("emailVerified");
            if (Boolean.TRUE.equals(emailVerified)) {
                throw new ValidationException("Esta cuenta ya fue activada previamente");
            }

            // 4. Marcar email como verificado
            keycloakAdminService.marcarEmailComoVerificado(userId, adminToken);
            log.info("Email verificado para userId: {}", userId);

            // 5. Establecer contraseña definitiva (NO temporal)
            keycloakClient.cambiarPassword(userId, newPassword, false, adminToken);
            log.info("Contraseña definitiva establecida para userId: {}", userId);

            // 6. Eliminar la required action UPDATE_PASSWORD (si existe)
            keycloakClient.eliminarRequiredAction(userId, "UPDATE_PASSWORD", adminToken);
            log.info("Required action UPDATE_PASSWORD eliminada para userId: {}", userId);

            // 7. Enviar email de confirmación
            String username = (String) user.get("username");
            emailService.enviarEmailCuentaActivada(email, username);

            log.info("✅ Cuenta activada completamente para userId: {}", userId);

        } catch (Exception e) {
            log.error("❌ Error activando cuenta: {}", e.getMessage(), e);
            throw new RuntimeException("No se pudo activar la cuenta: " + e.getMessage(), e);
        }
    }

    /**
     * Reenvía el email de activación si el token expiró
     */
    public void resendActivation(String email) {
        log.info("Reenvío de activación solicitado para email: {}", email);

        String adminToken = keycloakAdminService.getAdminToken();

        try {
            // Buscar usuario por email
            Map<String, Object> user = keycloakClient.buscarUsuarioPorEmail(email, adminToken);

            if (user == null) {
                // Por seguridad, no revelar si el email existe o no
                log.warn("Email no encontrado: {}, pero respondemos OK por seguridad", email);
                return;
            }

            String userId = (String) user.get("id");
            String username = (String) user.get("username");
            Boolean emailVerified = (Boolean) user.get("emailVerified");

            // Verificar que el usuario NO esté ya activado
            if (Boolean.TRUE.equals(emailVerified)) {
                log.warn("Usuario {} ya tiene email verificado, no se reenvía activación", username);
                throw new ValidationException("Esta cuenta ya fue activada. Podés iniciar sesión normalmente.");
            }

            // Generar nuevo token y reenviar email
            String newToken = jwtUtil.generateVerificationToken(userId, email);
            emailService.enviarEmailActivacion(email, username, newToken);

            log.info("Email de activación reenviado a: {}", email);

        } catch (ValidationException e) {
            throw e; // Propagar validaciones
        } catch (Exception e) {
            log.error("Error al reenviar activación para {}: {}", email, e.getMessage(), e);
            // Por seguridad, no lanzar excepción al frontend
        }
    }

    /**
     * DEPRECATED: Mantener por compatibilidad pero ya no se usa
     * Usar activateAccount() en su lugar
     */
    @Deprecated
    public void verifyTokenAndMarkEmail(String token) {
        String userId = jwtUtil.validateAndGetUserId(token);
        String adminToken = keycloakAdminService.getAdminToken();
        keycloakAdminService.marcarEmailComoVerificado(userId, adminToken);
        log.warn("⚠️ Método verifyTokenAndMarkEmail() está deprecado. Usar activateAccount() en su lugar.");
        log.info("Usuario {} marcado como email verificado en Keycloak", userId);
    }
}