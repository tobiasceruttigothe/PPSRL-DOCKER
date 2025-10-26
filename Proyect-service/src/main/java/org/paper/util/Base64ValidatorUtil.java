package org.paper.util;

import lombok.extern.slf4j.Slf4j;
import org.paper.exception.FileProcessingException;
import org.paper.exception.ValidationException;
import org.springframework.stereotype.Component;

import java.util.Base64;

/**
 * Utilidad para validar y procesar archivos en formato base64.
 *
 * Centraliza toda la lógica de validación de base64 para evitar duplicación
 * en múltiples servicios (Logos, Plantillas, Diseños, etc.)
 */
@Slf4j
@Component
public class Base64ValidatorUtil {

    // Tamaños máximos por defecto (en bytes)
    public static final long MAX_SIZE_5MB = 5 * 1024 * 1024;   // Para logos
    public static final long MAX_SIZE_10MB = 10 * 1024 * 1024; // Para plantillas/diseños

    /**
     * Valida el formato y tamaño del base64
     *
     * @param base64 String con el contenido base64 (puede incluir prefijo data:image/...)
     * @param fileName Nombre del archivo (para logging y mensajes de error)
     * @param fieldName Nombre del campo (para mensajes de error específicos)
     * @param maxSizeBytes Tamaño máximo permitido en bytes
     * @throws ValidationException si el base64 está vacío o es null
     * @throws FileProcessingException si el base64 es inválido o excede el tamaño
     */
    public void validateBase64(String base64, String fileName, String fieldName, long maxSizeBytes) {
        // Validar que no esté vacío
        if (base64 == null || base64.trim().isEmpty()) {
            throw new ValidationException(fieldName, "El campo no puede estar vacío");
        }

        try {
            // Limpiar y decodificar
            byte[] decodedBytes = Base64.getDecoder().decode(cleanBase64(base64));

            // Validar tamaño
            if (decodedBytes.length > maxSizeBytes) {
                double sizeMB = decodedBytes.length / (1024.0 * 1024.0);
                double maxSizeMB = maxSizeBytes / (1024.0 * 1024.0);

                String message = String.format(
                        "El archivo excede el tamaño máximo permitido (%.0fMB). Tamaño: %.2f MB",
                        maxSizeMB,
                        sizeMB
                );

                log.error("Archivo demasiado grande: {} - {}", fileName, message);
                throw new FileProcessingException(fileName, "validar tamaño", message);
            }

            log.debug("Base64 validado correctamente para: {} (tamaño: {} bytes)",
                    fileName, decodedBytes.length);

        } catch (IllegalArgumentException e) {
            log.error("Base64 inválido para archivo: {}", fileName, e);
            throw new FileProcessingException(
                    fileName,
                    "decodificar base64",
                    "El formato base64 es inválido"
            );
        }
    }

    /**
     * Valida base64 con tamaño máximo de 5MB (para logos)
     */
    public void validateBase64ForLogo(String base64, String fileName) {
        validateBase64(base64, fileName, "base64Logo", MAX_SIZE_5MB);
    }

    /**
     * Valida base64 con tamaño máximo de 10MB (para plantillas y diseños)
     */
    public void validateBase64ForPlantillaOrDiseno(String base64, String fileName) {
        validateBase64(base64, fileName, "base64", MAX_SIZE_10MB);
    }

    /**
     * Calcula el tamaño en bytes del base64
     *
     * @param base64 String con el contenido base64
     * @return Tamaño en bytes, o 0 si hay error
     */
    public long calculateBase64Size(String base64) {
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(cleanBase64(base64));
            return decodedBytes.length;
        } catch (IllegalArgumentException e) {
            log.warn("Error calculando tamaño del base64, retornando 0", e);
            return 0L;
        }
    }

    /**
     * Limpia el base64 eliminando prefijos como "data:image/png;base64,"
     *
     * @param base64 String con el contenido base64 (puede incluir prefijo)
     * @return String con base64 limpio (solo el contenido codificado)
     */
    public String cleanBase64(String base64) {
        if (base64 == null) {
            return null;
        }

        // Si contiene coma, asumir que tiene prefijo y extraer solo el base64
        if (base64.contains(",")) {
            return base64.substring(base64.indexOf(",") + 1);
        }

        return base64;
    }

    /**
     * Verifica si un string es un base64 válido
     *
     * @param base64 String a validar
     * @return true si es válido, false si no
     */
    public boolean isValidBase64(String base64) {
        if (base64 == null || base64.trim().isEmpty()) {
            return false;
        }

        try {
            Base64.getDecoder().decode(cleanBase64(base64));
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Extrae el tipo MIME del prefijo base64 (si existe)
     *
     * Ejemplo: "data:image/png;base64,..." → "image/png"
     *
     * @param base64 String con el contenido base64
     * @return Tipo MIME o null si no tiene prefijo
     */
    public String extractMimeType(String base64) {
        if (base64 == null || !base64.startsWith("data:")) {
            return null;
        }

        try {
            // Formato: data:image/png;base64,XXXXX
            String prefix = base64.substring(5, base64.indexOf(";"));
            return prefix;
        } catch (Exception e) {
            log.warn("No se pudo extraer MIME type del base64", e);
            return null;
        }
    }
}