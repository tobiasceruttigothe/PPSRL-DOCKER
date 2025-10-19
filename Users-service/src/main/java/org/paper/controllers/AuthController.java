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
import org.paper.dto.ChangePasswordDTO;
import org.paper.dto.ErrorResponse;
import org.paper.dto.ForgotPasswordRequestDTO;
import org.paper.dto.ResetPasswordDTO;
import org.paper.services.EmailVerificationService;
import org.paper.services.PasswordRecoveryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Slf4j
@Tag(name = "Autenticación", description = "Endpoints públicos de autenticación y verificación")
public class AuthController {

    private final EmailVerificationService verificationService;
    private final PasswordRecoveryService passwordRecoveryService;

    public AuthController(EmailVerificationService verificationService,
                          PasswordRecoveryService passwordRecoveryService) {
        this.verificationService = verificationService;
        this.passwordRecoveryService = passwordRecoveryService;
    }

    @PostMapping("/verify-email")
    @Operation(
            summary = "Verificar email del usuario",
            description = """
            Verifica el email de un usuario usando el token JWT enviado por correo.
            
            **Flujo:**
            1. Usuario recibe email con link: `http://frontend.com/verify-email?token=XXX`
            2. Frontend extrae el token del query param
            3. Frontend llama a este endpoint con el token
            4. Backend valida el token y marca el email como verificado en Keycloak
            
            ⚠️ **Nota:** Este endpoint es público (no requiere autenticación previa).
            
            **Token válido por:** 24 horas
            """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Email verificado correctamente. El usuario ya puede iniciar sesión.",
                    content = @Content(mediaType = "text/plain")
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Token inválido, expirado o mal formado",
                    content = @Content(mediaType = "text/plain")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Error interno al verificar el email",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<String> verifyEmail(
            @Parameter(
                    description = "Token JWT de verificación recibido por email",
                    required = true
            )
            @RequestParam("token") String token) {
        try {
            log.info("Verificación de email solicitada");
            verificationService.verifyTokenAndMarkEmail(token);
            return ResponseEntity.ok("Email verificado correctamente");
        } catch (Exception e) {
            log.error("Error verificando token: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Token inválido o expirado");
        }
    }

    @PostMapping("/forgot-password")
    @Operation(
            summary = "Solicitar recuperación de contraseña",
            description = """
            Envía un email con un link para restablecer la contraseña.
            
            **Flujo:**
            1. Usuario hace clic en "Olvidé mi contraseña"
            2. Frontend envía el email a este endpoint
            3. Backend genera token JWT y envía email con link
            4. Usuario recibe email con link: `http://frontend.com/reset-password?token=XXX`
            
            ⚠️ **Nota:** Por seguridad, siempre retorna 200 OK aunque el email no exista.
            
            **Token válido por:** 1 hora
            """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Si el email existe, se ha enviado un correo de recuperación",
                    content = @Content(mediaType = "text/plain")
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Email inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<String> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequestDTO request) {
        log.info("Solicitud de recuperación de contraseña para: {}", request.getEmail());
        passwordRecoveryService.solicitarRecuperacionPassword(request.getEmail());
        return ResponseEntity.ok("Si el email existe en el sistema, recibirás un correo con instrucciones");
    }

    @PostMapping("/reset-password")
    @Operation(
            summary = "Restablecer contraseña con token",
            description = """
            Restablece la contraseña usando el token recibido por email.
            
            **Flujo:**
            1. Usuario hace clic en el link del email de recuperación
            2. Frontend muestra formulario de nueva contraseña
            3. Frontend envía token + nueva contraseña a este endpoint
            4. Backend valida el token y cambia la contraseña
            
            ⚠️ **Nota:** El token es de un solo uso y expira en 1 hora.
            """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Contraseña restablecida correctamente",
                    content = @Content(mediaType = "text/plain")
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Token inválido, expirado o datos inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<String> resetPassword(
            @Valid @RequestBody ResetPasswordDTO request) {
        try {
            log.info("Intento de reseteo de contraseña");
            passwordRecoveryService.resetearPassword(request.getToken(), request.getNewPassword());
            return ResponseEntity.ok("Contraseña restablecida correctamente");
        } catch (Exception e) {
            log.error("Error al resetear contraseña: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Token inválido o expirado");
        }
    }

    @PostMapping("/change-temporary-password")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
            summary = "Cambiar contraseña temporal (requiere autenticación)",
            description = """
            Permite al usuario cambiar su contraseña temporal por una definitiva.
            
            **Flujo:**
            1. Usuario inicia sesión con contraseña temporal
            2. Keycloak lo obliga a cambiarla (o frontend detecta que es temporal)
            3. Usuario ingresa contraseña actual (temporal) y nueva contraseña
            4. Frontend envía ambas contraseñas a este endpoint (con token JWT de sesión)
            5. Backend cambia la contraseña a definitiva
            
            ⚠️ **Requiere:** Token JWT de autenticación válido
            """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Contraseña cambiada correctamente",
                    content = @Content(mediaType = "text/plain")
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Contraseña actual incorrecta o nueva contraseña inválida",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado (token inválido o expirado)"
            )
    })
    public ResponseEntity<String> changeTemporaryPassword(
            @Valid @RequestBody ChangePasswordDTO request,
            Authentication authentication) {
        try {
            // Obtener userId del JWT de Keycloak
            Jwt jwt = (Jwt) authentication.getPrincipal();
            String userId = jwt.getSubject();

            log.info("Cambio de contraseña temporal solicitado para userId: {}", userId);

            passwordRecoveryService.cambiarPasswordTemporal(
                    userId,
                    request.getCurrentPassword(),
                    request.getNewPassword()
            );

            return ResponseEntity.ok("Contraseña cambiada correctamente");
        } catch (Exception e) {
            log.error("Error al cambiar contraseña temporal: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("No se pudo cambiar la contraseña. Verificá que la contraseña actual sea correcta.");
        }
    }
}