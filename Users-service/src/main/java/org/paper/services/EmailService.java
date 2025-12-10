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

    // Estilo de botón reutilizable y mejorado
    private final String botonAzul = """
        background-color: #016add;
        color: #ffffff;
        border: 1px solid #016add;
        font-weight: bold;
        padding: 12px 32px;
        border-radius: 50px;
        text-decoration: none;
        display: inline-block;
        font-size: 15px;
        font-family: Arial, sans-serif;
        text-align: center;
        box-shadow: 0 4px 6px rgba(1, 106, 221, 0.2);
    """;

    // Estilo base para el contenedor principal (card)
    private final String contenedorBase = """
        background-color: #ffffff;
        border-radius: 12px;
        padding: 40px 30px;
        box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
        text-align: center;
        max-width: 600px;
        margin: 0 auto;
    """;

    // ============================================================
    // EMAIL ACTIVACIÓN DE CUENTA
    // ============================================================

    public void enviarEmailActivacion(String destinatario, String username, String token) {
        try {
            String activationLink = frontendUrl + "/activate-account?token=" + token;

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(destinatario);
            helper.setSubject("Activá tu cuenta - Paper SRL");

            String html = """
                <!DOCTYPE html>
                <html>
                <body style="margin:0; padding:0; background-color:#f4f6f8; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;">
                    <table width="100%%" border="0" cellspacing="0" cellpadding="0" style="background-color:#f4f6f8;">
                        <tr>
                            <td align="center" style="padding: 40px 20px;">
                                
                                <div style="%s"> <h1 style="color: #016add; margin: 0 0 20px 0; font-size: 24px;">¡Bienvenido a Paper SRL!</h1>
                                    
                                    <p style="color: #555555; font-size: 16px; line-height: 1.6; margin: 0 0 30px 0;">
                                        Hola <strong>%s</strong>,<br>
                                        Tu cuenta ha sido creada exitosamente. Para comenzar a utilizar la plataforma, por favor activá tu cuenta haciendo clic en el siguiente botón.
                                    </p>

                                    <table width="100%%" border="0" cellspacing="0" cellpadding="0">
                                        <tr>
                                            <td align="center" style="padding-bottom: 30px;">
                                                <a href="%s" target="_blank" style="%s">Activar mi cuenta</a>
                                            </td>
                                        </tr>
                                    </table>

                                    <p style="font-size: 13px; color: #888888; margin: 0 0 10px 0;">
                                        Si el botón no funciona, podés copiar y pegar este enlace:
                                    </p>
                                    <p style="font-size: 13px; margin: 0 0 30px 0; word-break: break-all;">
                                        <a href="%s" style="color: #016add; text-decoration: none;">%s</a>
                                    </p>

                                    <div style="background-color: #fffbf0; border: 1px solid #ffeeba; border-radius: 8px; padding: 15px; text-align: left;">
                                        <p style="margin: 0; color: #856404; font-size: 14px; text-align: center;">
                                            <strong>⏰ Este enlace expira en 24 horas.</strong>
                                        </p>
                                    </div>

                                </div>

                                <p style="text-align: center; font-size: 12px; color: #999999; margin-top: 25px;">
                                    © 2025 Paper SRL - Sistema de Gestión de Diseños
                                </p>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
            """.formatted(contenedorBase, username, activationLink, botonAzul, activationLink, activationLink);

            helper.setText(html, true);
            mailSender.send(message);

        } catch (Exception e) {
            throw new RuntimeException("No se pudo enviar el email de activación");
        }
    }

    // ============================================================
    // EMAIL CONFIRMACIÓN DE CUENTA ACTIVADA (CORREGIDO EL TICK)
    // ============================================================

    public void enviarEmailCuentaActivada(String destinatario, String username) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(destinatario);
            helper.setSubject("Cuenta activada - Paper SRL");

            // Truco CSS: display inline-block y line-height igual al height centra verticalmente sin Flexbox
            String estiloTick = """
                display: inline-block;
                width: 80px;
                height: 80px;
                line-height: 80px;
                background-color: #27ae60;
                border-radius: 50%%;
                color: #ffffff;
                font-size: 40px;
                text-align: center;
                margin-bottom: 20px;
                box-shadow: 0 4px 10px rgba(39, 174, 96, 0.3);
            """;

            String html = """
                <!DOCTYPE html>
                <html>
                <body style="margin:0; padding:0; background-color:#f4f6f8; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;">
                    <table width="100%%" border="0" cellspacing="0" cellpadding="0" style="background-color:#f4f6f8;">
                        <tr>
                            <td align="center" style="padding: 40px 20px;">
                                
                                <div style="%s"> <div style="text-align: center;">
                                        <span style="%s">✓</span>
                                    </div>

                                    <h1 style="color: #27ae60; margin: 0 0 15px 0; font-size: 24px;">¡Tu cuenta está activa!</h1>

                                    <p style="color: #555555; font-size: 16px; margin: 0 0 30px 0;">
                                        Hola <strong>%s</strong>, el proceso ha finalizado correctamente.
                                    </p>

                                    <table width="100%%" border="0" cellspacing="0" cellpadding="0">
                                        <tr>
                                            <td align="center" style="padding-bottom: 30px;">
                                                <a href="%s/login" target="_blank" style="%s">Iniciar sesión</a>
                                            </td>
                                        </tr>
                                    </table>

                                    <div style="background-color: #e8f5e9; border: 1px solid #c8e6c9; border-radius: 8px; padding: 20px; text-align: left;">
                                        <p style="margin: 0 0 10px 0; color: #2e7d32; font-size: 14px; font-weight: bold;">
                                            🔐 Tips de seguridad:
                                        </p>
                                        <ul style="margin: 0; padding-left: 20px; color: #388e3c; font-size: 14px;">
                                            <li style="margin-bottom: 5px;">No compartas tu contraseña con nadie.</li>
                                            <li style="margin-bottom: 5px;">Si olvidás tu clave, usá la opción de recuperación.</li>
                                        </ul>
                                    </div>

                                </div>

                                <p style="text-align: center; font-size: 12px; color: #999999; margin-top: 25px;">
                                    © 2025 Paper SRL
                                </p>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
            """.formatted(contenedorBase, estiloTick, username, frontendUrl, botonAzul);

            helper.setText(html, true);
            mailSender.send(message);

        } catch (Exception ignored) {}
    }

    // ============================================================
    // EMAIL RECUPERACIÓN DE CONTRASEÑA
    // ============================================================

    public void enviarEmailRecuperacionPassword(String destinatario, String username, String token) {
        try {
            String resetLink = frontendUrl + "/reset-password?token=" + token;

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(destinatario);
            helper.setSubject("Recuperación de contraseña - Paper SRL");

            String html = """
                <!DOCTYPE html>
                <html>
                <body style="margin:0; padding:0; background-color:#f4f6f8; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;">
                    <table width="100%%" border="0" cellspacing="0" cellpadding="0" style="background-color:#f4f6f8;">
                        <tr>
                            <td align="center" style="padding: 40px 20px;">
                                
                                <div style="%s">
                                    
                                    <h1 style="color: #016add; margin: 0 0 20px 0; font-size: 24px;">Recuperar contraseña</h1>

                                    <p style="color: #555555; font-size: 16px; margin: 0 0 30px 0;">
                                        Hola <strong>%s</strong>, hemos recibido una solicitud para restablecer la contraseña de tu cuenta.
                                    </p>

                                    <table width="100%%" border="0" cellspacing="0" cellpadding="0">
                                        <tr>
                                            <td align="center" style="padding-bottom: 30px;">
                                                <a href="%s" target="_blank" style="%s">Restablecer contraseña</a>
                                            </td>
                                        </tr>
                                    </table>

                                    <p style="font-size: 13px; color: #888888; margin: 0 0 10px 0;">
                                        Si no fuiste vos, ignorá este mensaje. Si el botón no funciona:
                                    </p>
                                    <p style="font-size: 13px; margin: 0 0 30px 0; word-break: break-all;">
                                        <a href="%s" style="color: #016add; text-decoration: none;">%s</a>
                                    </p>

                                    <div style="background-color: #fce8e6; border: 1px solid #fadbd8; border-radius: 8px; padding: 15px;">
                                        <p style="margin: 0; color: #c0392b; font-size: 14px; font-weight: bold;">
                                            ⏰ Este enlace expira en 1 hora.
                                        </p>
                                    </div>

                                </div>

                                <p style="text-align: center; font-size: 12px; color: #999999; margin-top: 25px;">
                                    © 2025 Paper SRL
                                </p>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
            """.formatted(contenedorBase, username, resetLink, botonAzul, resetLink, resetLink);

            helper.setText(html, true);
            mailSender.send(message);

        } catch (Exception e) {
            throw new RuntimeException("No se pudo enviar email");
        }
    }

    // ============================================================
    // EMAIL CONFIRMACIÓN DE CONTRASEÑA ACTUALIZADA
    // ============================================================

    public void enviarEmailPasswordCambiada(String destinatario, String username) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(destinatario);
            helper.setSubject("Contraseña actualizada - Paper SRL");

            String html = """
                <!DOCTYPE html>
                <html>
                <body style="margin:0; padding:0; background-color:#f4f6f8; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;">
                    <table width="100%%" border="0" cellspacing="0" cellpadding="0" style="background-color:#f4f6f8;">
                        <tr>
                            <td align="center" style="padding: 40px 20px;">
                                
                                <div style="%s">
                                    
                                    <h1 style="color: #27ae60; margin: 0 0 20px 0; font-size: 24px;">Contraseña actualizada</h1>

                                    <p style="color: #555555; font-size: 16px; margin: 0 0 30px 0;">
                                        Hola <strong>%s</strong>, te confirmamos que tu contraseña ha sido modificada correctamente.
                                    </p>

                                    <div style="background-color: #fff3cd; border: 1px solid #ffeeba; border-radius: 8px; padding: 15px; margin-bottom: 30px; text-align: left;">
                                        <p style="margin: 0; color: #856404; font-size: 14px;">
                                            <strong>⚠️ ¿No fuiste vos?</strong><br>
                                            Si no realizaste este cambio, por favor contactá al administrador del sistema inmediatamente.
                                        </p>
                                    </div>

                                    <table width="100%%" border="0" cellspacing="0" cellpadding="0">
                                        <tr>
                                            <td align="center">
                                                <a href="%s/login" target="_blank" style="%s">Iniciar sesión</a>
                                            </td>
                                        </tr>
                                    </table>

                                </div>

                                <p style="text-align: center; font-size: 12px; color: #999999; margin-top: 25px;">
                                    © 2025 Paper SRL
                                </p>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
            """.formatted(contenedorBase, username, frontendUrl, botonAzul);

            helper.setText(html, true);
            mailSender.send(message);

        } catch (Exception ignored) {}
    }
}