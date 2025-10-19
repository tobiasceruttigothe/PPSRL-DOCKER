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

    public void enviarEmailVerificacion(String destinatario, String username, String token) {
        try {
            String verificationLink = frontendUrl + "/verify-email?token=" + token;

            log.info("Enviando email de verificación a {}", destinatario);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(destinatario);
            helper.setSubject("Verificación de email - Mi Aplicación");

            String htmlContent = """
                <html>
                <body>
                    <h2>Hola %s,</h2>
                    <p>Por favor verificá tu email haciendo clic en el siguiente botón:</p>
                    <p><a href="%s" style="background: #4CAF50; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;">Verificar Email</a></p>
                    <p>O copiá este enlace en tu navegador: %s</p>
                    <p>Este enlace expira en 24 horas.</p>
                </body>
                </html>
            """.formatted(username, verificationLink, verificationLink);

            helper.setText(htmlContent, true);

            mailSender.send(message);

            log.info("Email de verificación enviado a {}", destinatario);
        } catch (Exception e) {
            log.error("Error enviando email de verificación: {}", e.getMessage(), e);
            throw new RuntimeException("No se pudo enviar el email de verificación");
        }
    }

    public void enviarEmailRecuperacionPassword(String destinatario, String username, String token) {
        try {
            String resetLink = frontendUrl + "/reset-password?token=" + token;

            log.info("Enviando email de recuperación de contraseña a {}", destinatario);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(destinatario);
            helper.setSubject("Recuperación de contraseña - Mi Aplicación");

            String htmlContent = """
                <html>
                <body>
                    <h2>Hola %s,</h2>
                    <p>Recibimos una solicitud para restablecer tu contraseña.</p>
                    <p>Si fuiste vos, hacé clic en el siguiente botón para crear una nueva contraseña:</p>
                    <p><a href="%s" style="background: #FF5722; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;">Restablecer Contraseña</a></p>
                    <p>O copiá este enlace en tu navegador: %s</p>
                    <p><strong>Este enlace expira en 1 hora.</strong></p>
                    <p>Si no solicitaste este cambio, ignorá este correo y tu contraseña permanecerá sin cambios.</p>
                </body>
                </html>
            """.formatted(username, resetLink, resetLink);

            helper.setText(htmlContent, true);

            mailSender.send(message);

            log.info("Email de recuperación de contraseña enviado a {}", destinatario);
        } catch (Exception e) {
            log.error("Error enviando email de recuperación: {}", e.getMessage(), e);
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
            helper.setSubject("Contraseña actualizada - Mi Aplicación");

            String htmlContent = """
                <html>
                <body>
                    <h2>Hola %s,</h2>
                    <p>Tu contraseña ha sido cambiada exitosamente.</p>
                    <p>Si no realizaste este cambio, por favor contactá con el administrador inmediatamente.</p>
                    <p>Podés iniciar sesión con tu nueva contraseña en: <a href="%s">%s</a></p>
                </body>
                </html>
            """.formatted(username, frontendUrl, frontendUrl);

            helper.setText(htmlContent, true);

            mailSender.send(message);

            log.info("Email de confirmación enviado a {}", destinatario);
        } catch (Exception e) {
            log.error("Error enviando email de confirmación: {}", e.getMessage(), e);
            // No lanzar excepción aquí, es solo notificación
        }
    }
}