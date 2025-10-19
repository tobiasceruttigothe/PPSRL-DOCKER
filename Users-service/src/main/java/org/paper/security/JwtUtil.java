package org.paper.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final long verificationExpirationMs;
    private final long recoveryExpirationMs;

    public JwtUtil(@Value("${app.jwt.secret}") String jwtSecret,
                   @Value("${app.jwt.expiration-ms}") long verificationExpirationMs) {
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.verificationExpirationMs = verificationExpirationMs; // 24 horas
        this.recoveryExpirationMs = 3600000; // 1 hora en milisegundos
    }

    /**
     * Genera token para verificación de email (válido 24 horas)
     */
    public String generateVerificationToken(String userId, String email) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(userId)
                .claim("email", email)
                .claim("type", "email_verification")
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + verificationExpirationMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Genera token para recuperación de contraseña (válido 1 hora)
     */
    public String generatePasswordRecoveryToken(String userId, String email) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(userId)
                .claim("email", email)
                .claim("type", "password_recovery")
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + recoveryExpirationMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Valida y devuelve el userId del token
     */
    public String validateAndGetUserId(String token) {
        try {
            Jws<Claims> jws = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return jws.getBody().getSubject();
        } catch (ExpiredJwtException e) {
            throw new RuntimeException("El token ha expirado");
        } catch (JwtException e) {
            throw new RuntimeException("Token inválido");
        }
    }

    /**
     * Obtiene el email del token
     */
    public String getEmailFromToken(String token) {
        try {
            Jws<Claims> jws = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return jws.getBody().get("email", String.class);
        } catch (JwtException e) {
            throw new RuntimeException("Token inválido");
        }
    }

    /**
     * Obtiene el tipo de token (email_verification o password_recovery)
     */
    public String getTokenType(String token) {
        try {
            Jws<Claims> jws = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return jws.getBody().get("type", String.class);
        } catch (JwtException e) {
            throw new RuntimeException("Token inválido");
        }
    }
}