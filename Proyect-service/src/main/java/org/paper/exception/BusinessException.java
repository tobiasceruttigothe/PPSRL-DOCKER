package org.paper.exception;

/**
 * Excepción para errores de lógica de negocio
 */
public class BusinessException extends RuntimeException {
    private final String code;

    public BusinessException(String message) {
        super(message);
        this.code = "BUSINESS_ERROR";
    }

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.code = "BUSINESS_ERROR";
    }

    public String getCode() {
        return code;
    }
}
