package org.paper.services;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * NUEVO: Envía email para activar cuenta (reemplaza enviarEmailVerificacion)
     * El usuario debe establecer su contraseña definitiva desde este link
     */
    public void enviarEmailActivacion(String destinatario, String username, String token) {
        try {
            String activationLink = frontendUrl + "/activate-account?token=" + token;

            log.info("Enviando email de activación a {}", destinatario);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(destinatario);
            helper.setSubject("Activá tu cuenta - Paper SRL");

            String htmlContent = """
                <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                        <h2 style="color: #2c3e50;">¡Bienvenido a Paper SRL, %s!</h2>
                        
                        <p>Tu cuenta ha sido creada exitosamente. Para comenzar a usar la plataforma, 
                        necesitás activar tu cuenta y establecer tu contraseña.</p>
                        
                        <p style="margin: 30px 0;">
                            <a href="%s" 
                               style="background: #3498db; 
                                      color: white; 
                                      padding: 12px 30px; 
                                      text-decoration: none; 
                                      border-radius: 5px;
                                      display: inline-block;
                                      font-weight: bold;">
                                Activar mi cuenta
                            </a>
                        </p>
                        
                        <p style="color: #7f8c8d; font-size: 14px;">
                            Si el botón no funciona, copiá y pegá este enlace en tu navegador:<br>
                            <a href="%s" style="color: #3498db;">%s</a>
                        </p>
                        
                        <div style="background: #fff3cd; 
                                    border-left: 4px solid #ffc107; 
                                    padding: 15px; 
                                    margin: 20px 0;">
                            <strong>⏰ Este enlace expira en 24 horas.</strong><br>
                            Si no activás tu cuenta en este período, deberás solicitar un nuevo enlace.
                        </div>
                        
                        <p style="color: #7f8c8d; font-size: 13px; margin-top: 30px;">
                            Si no solicitaste esta cuenta, podés ignorar este correo.
                        </p>
                        
                        <hr style="border: none; border-top: 1px solid #ecf0f1; margin: 20px 0;">
                        
                        <p style="color: #95a5a6; font-size: 12px; text-align: center;">
                            © 2025 Paper SRL - Sistema de Gestión de Diseños
                        </p>
                    </div>
                </body>
                </html>
            """.formatted(username, activationLink, activationLink, activationLink);

            helper.setText(htmlContent, true);

            mailSender.send(message);

            log.info("✅ Email de activación enviado a {}", destinatario);
        } catch (Exception e) {
            log.error("❌ Error enviando email de activación: {}", e.getMessage(), e);
            throw new RuntimeException("No se pudo enviar el email de activación");
        }
    }

    /**
     * NUEVO: Envía confirmación de cuenta activada exitosamente
     */
    public void enviarEmailCuentaActivada(String destinatario, String username) {
        try {
            log.info("Enviando email de confirmación de activación a {}", destinatario);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(destinatario);
            helper.setSubject("Cuenta activada - Paper SRL");

            String htmlContent = """
                <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                        <div style="text-align: center; margin-bottom: 30px;">
                            <div style="background: #27ae60; 
                                        color: white; 
                                        width: 80px; 
                                        height: 80px; 
                                        border-radius: 50%%; 
                                        display: inline-flex; 
                                        align-items: center; 
                                        justify-content: center;
                                        font-size: 40px;">
                                ✓
                            </div>
                        </div>
                        
                        <h2 style="color: #27ae60; text-align: center;">
                            ¡Tu cuenta está activa!
                        </h2>
                        
                        <p>Hola <strong>%s</strong>,</p>
                        
                        <p>Tu cuenta en Paper SRL ha sido activada exitosamente. 
                        Ya podés iniciar sesión con las credenciales que estableciste.</p>
                        
                        <p style="margin: 30px 0; text-align: center;">
                            <a href="%s/login" 
                               style="background: #3498db; 
                                      color: white; 
                                      padding: 12px 30px; 
                                      text-decoration: none; 
                                      border-radius: 5px;
                                      display: inline-block;
                                      font-weight: bold;">
                                Iniciar sesión
                            </a>
                        </p>
                        
                        <div style="background: #e8f5e9; 
                                    border-left: 4px solid #27ae60; 
                                    padding: 15px; 
                                    margin: 20px 0;">
                            <strong>🔐 Recordá:</strong><br>
                            • Guardá tu contraseña en un lugar seguro<br>
                            • No compartas tus credenciales con nadie<br>
                            • Si olvidás tu contraseña, podés restablecerla desde el login
                        </div>
                        
                        <p>Si tenés alguna consulta, no dudes en contactarnos.</p>
                        
                        <hr style="border: none; border-top: 1px solid #ecf0f1; margin: 20px 0;">
                        
                        <p style="color: #95a5a6; font-size: 12px; text-align: center;">
                            © 2025 Paper SRL - Sistema de Gestión de Diseños
                        </p>
                    </div>
                </body>
                </html>
            """.formatted(username, frontendUrl);

            helper.setText(htmlContent, true);

            mailSender.send(message);

            log.info("✅ Email de confirmación enviado a {}", destinatario);
        } catch (Exception e) {
            log.error("❌ Error enviando email de confirmación: {}", e.getMessage(), e);
            // No lanzar excepción, es solo notificación
        }
    }

    /**
     * DEPRECATED: Mantener por compatibilidad
     * Usar enviarEmailActivacion() en su lugar
     */
    @Deprecated
    public void enviarEmailVerificacion(String destinatario, String username, String token) {
        log.warn("⚠️ enviarEmailVerificacion() está deprecado. Usar enviarEmailActivacion()");
        enviarEmailActivacion(destinatario, username, token);
    }

    // ==================== MÉTODOS DE RECUPERACIÓN DE CONTRASEÑA ====================

    public void enviarEmailRecuperacionPassword(String destinatario, String username, String token) {
        try {
            String resetLink = frontendUrl + "/reset-password?token=" + token;

            log.info("Enviando email de recuperación de contraseña a {}", destinatario);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(destinatario);
            helper.setSubject("Recuperación de contraseña - Paper SRL");

            String htmlContent = """
                <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                        <h2 style="color: #e74c3c;">Recuperación de contraseña</h2>
                        
                        <p>Hola <strong>%s</strong>,</p>
                        
                        <p>Recibimos una solicitud para restablecer tu contraseña.</p>
                        
                        <p>Si fuiste vos, hacé clic en el siguiente botón para crear una nueva contraseña:</p>
                        
                        <p style="margin: 30px 0;">
                            <a href="%s" 
                               style="background: #e74c3c; 
                                      color: white; 
                                      padding: 12px 30px; 
                                      text-decoration: none; 
                                      border-radius: 5px;
                                      display: inline-block;
                                      font-weight: bold;">
                                Restablecer contraseña
                            </a>
                        </p>
                        
                        <p style="color: #7f8c8d; font-size: 14px;">
                            O copiá este enlace en tu navegador:<br>
                            <a href="%s" style="color: #e74c3c;">%s</a>
                        </p>
                        
                        <div style="background: #ffebee; 
                                    border-left: 4px solid #e74c3c; 
                                    padding: 15px; 
                                    margin: 20px 0;">
                            <strong>⏰ Este enlace expira en 1 hora.</strong>
                        </div>
                        
                        <p style="color: #7f8c8d;">
                            Si no solicitaste este cambio, ignorá este correo y tu contraseña 
                            permanecerá sin cambios.
                        </p>
                        
                        <hr style="border: none; border-top: 1px solid #ecf0f1; margin: 20px 0;">
                        
                        <p style="color: #95a5a6; font-size: 12px; text-align: center;">
                            © 2025 Paper SRL - Sistema de Gestión de Diseños
                        </p>
                    </div>
                </body>
                </html>
            """.formatted(username, resetLink, resetLink, resetLink);

            helper.setText(htmlContent, true);

            mailSender.send(message);

            log.info("✅ Email de recuperación de contraseña enviado a {}", destinatario);
        } catch (Exception e) {
            log.error("❌ Error enviando email de recuperación: {}", e.getMessage(), e);
            throw new RuntimeException("No se pudo enviar el email de recuperación de contraseña");
        }
    }

    public void enviarEmailPasswordCambiada(String destinatario, String username) {
        try {
            log.info("Enviando email de confirmación de cambio de contraseña a {}", destinatario);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(destinatario);
            helper.setSubject("Contraseña actualizada - Paper SRL");

            String htmlContent = """
                <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                        <h2 style="color: #27ae60;">Contraseña actualizada</h2>
                        
                        <p>Hola <strong>%s</strong>,</p>
                        
                        <p>Tu contraseña ha sido cambiada exitosamente.</p>
                        
                        <div style="background: #fff3cd; 
                                    border-left: 4px solid #ffc107; 
                                    padding: 15px; 
                                    margin: 20px 0;">
                            <strong>⚠️ ¿No fuiste vos?</strong><br>
                            Si no realizaste este cambio, contactá con el administrador inmediatamente.
                        </div>
                        
                        <p>Podés iniciar sesión con tu nueva contraseña en:</p>
                        
                        <p style="margin: 30px 0;">
                            <a href="%s/login" 
                               style="background: #3498db; 
                                      color: white; 
                                      padding: 12px 30px; 
                                      text-decoration: none; 
                                      border-radius: 5px;
                                      display: inline-block;
                                      font-weight: bold;">
                                Iniciar sesión
                            </a>
                        </p>
                        
                        <hr style="border: none; border-top: 1px solid #ecf0f1; margin: 20px 0;">
                        
                        <p style="color: #95a5a6; font-size: 12px; text-align: center;">
                            © 2025 Paper SRL - Sistema de Gestión de Diseños
                        </p>
                    </div>
                </body>
                </html>
            """.formatted(username, frontendUrl);

            helper.setText(htmlContent, true);

            mailSender.send(message);

            log.info("✅ Email de confirmación enviado a {}", destinatario);
        } catch (Exception e) {
            log.error("❌ Error enviando email de confirmación: {}", e.getMessage(), e);
            // No lanzar excepción aquí, es solo notificación
        }
    }
}