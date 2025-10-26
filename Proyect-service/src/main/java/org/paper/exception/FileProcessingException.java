package org.paper.exception;

/**
 * Excepción para errores al procesar archivos (imágenes base64)
 */
public class FileProcessingException extends RuntimeException {
    private final String fileName;
    private final String operation;

    public FileProcessingException(String fileName, String operation, String message) {
        super(String.format("Error al %s el archivo '%s': %s", operation, fileName, message));
        this.fileName = fileName;
        this.operation = operation;
    }

    public FileProcessingException(String fileName, String operation, Throwable cause) {
        super(String.format("Error al %s el archivo '%s'", operation, fileName), cause);
        this.fileName = fileName;
        this.operation = operation;
    }

    public String getFileName() {
        return fileName;
    }

    public String getOperation() {
        return operation;
    }
}
