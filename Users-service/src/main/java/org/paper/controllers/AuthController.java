package org.paper.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.paper.dto.*;
import org.paper.services.EmailVerificationService;
import org.paper.services.PasswordRecoveryService;
import org.springframework.http.ResponseEntity;
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

    // ==================== ACTIVACIÓN DE CUENTA ====================

    @PostMapping("/activate-account")
    @Operation(
            summary = "Activar cuenta de usuario",
            description = """
            **✨ Activa la cuenta del usuario en un solo paso.**
            
            Este endpoint combina:
            1. ✅ Verificación del email
            2. 🔐 Establecimiento de contraseña definitiva
            3. 🎯 Eliminación de required actions de Keycloak
            
            **Flujo completo:**
            1. Admin crea usuario → Usuario recibe email con link de activación
            2. Usuario hace click en el link: `http://frontend.com/activate-account?token=XXX`
            3. Frontend muestra formulario donde el usuario ingresa su **nueva contraseña**
            4. Frontend envía token + nueva contraseña a este endpoint
            5. Backend activa todo: verifica email, establece password, habilita login
            6. Usuario recibe email de confirmación
            7. ✅ Usuario puede iniciar sesión inmediatamente
            
            ⚠️ **Nota:** Este endpoint es público (no requiere autenticación previa).
            
            **Token válido por:** 24 horas
            
            **Ventajas:**
            - ✅ Más seguro: No se envían contraseñas por email
            - ✅ Mejor UX: Todo en un solo paso
            - ✅ Menos confusión: No hay passwords temporales
            """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Cuenta activada correctamente. El usuario ya puede iniciar sesión.",
                    content = @Content(mediaType = "text/plain")
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Token inválido, expirado, cuenta ya activada, o contraseña inválida",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Error interno al activar la cuenta",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<String> activateAccount(
            @Valid @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = """
                    **Token:** JWT recibido por email
                    **newPassword:** Contraseña que el usuario desea establecer (min 8 caracteres)
                    """,
                    required = true
            )
            ActivateAccountDTO request) {
        try {
            log.info("Solicitud de activación de cuenta recibida");
            verificationService.activateAccount(request.getToken(), request.getNewPassword());
            return ResponseEntity.ok("✅ Cuenta activada correctamente. Ya podés iniciar sesión con tu nueva contraseña.");
        } catch (Exception e) {
            log.error("❌ Error activando cuenta: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("❌ " + e.getMessage());
        }
    }

    @PostMapping("/resend-activation")
    @Operation(
            summary = "Reenviar email de activación",
            description = """
            **🔄 Solicita un nuevo email de activación si el token expiró (24 horas).**
            
            **Flujo:**
            1. Usuario intenta activar cuenta con token expirado → Ve mensaje de error
            2. Frontend muestra botón "Solicitar nuevo link de activación"
            3. Usuario ingresa su email
            4. Backend valida que:
               - El email existe en el sistema
               - La cuenta NO esté ya activada
            5. Se genera nuevo token y se reenvía el email
            
            ⚠️ **Nota:** Por seguridad, siempre retorna 200 OK aunque el email no exista
            o la cuenta ya esté activada (no revelamos información).
            
            **Validaciones:**
            - ✅ Email debe existir en el sistema
            - ✅ Cuenta debe NO estar activada (emailVerified = false)
            - ❌ Si ya está activada, se retorna mensaje indicando que puede hacer login
            """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Si el email existe y no está activado, se ha enviado un nuevo correo",
                    content = @Content(mediaType = "text/plain")
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Email inválido o cuenta ya activada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<String> resendActivation(
            @Valid @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Email del usuario que necesita reactivar su cuenta",
                    required = true
            )
            ResendActivationDTO request) {
        try {
            log.info("Solicitud de reenvío de activación para: {}", request.getEmail());
            verificationService.resendActivation(request.getEmail());
            return ResponseEntity.ok("✅ Si tu email está registrado y la cuenta no está activada, recibirás un nuevo correo.");
        } catch (Exception e) {
            log.error("Error en reenvío de activación: {}", e.getMessage());
            return ResponseEntity.badRequest().body("❌ " + e.getMessage());
        }
    }

    // ==================== RECUPERACIÓN DE CONTRASEÑA ====================

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
            
            **Diferencia con /resend-activation:**
            - 🔄 `/resend-activation`: Para cuentas NO activadas (sin password aún)
            - 🔐 `/forgot-password`: Para cuentas YA activadas (olvido de password)
            
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
}