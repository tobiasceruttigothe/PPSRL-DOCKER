package org.paper.exception;

/**
 * Excepción cuando un usuario intenta acceder a un recurso que no le pertenece
 */
public class UnauthorizedAccessException extends RuntimeException {
    private final String userId;
    private final String resource;

    public UnauthorizedAccessException(String userId, String resource) {
        super(String.format("Usuario '%s' no tiene permiso para acceder a '%s'", userId, resource));
        this.userId = userId;
        this.resource = resource;
    }

    public String getUserId() {
        return userId;
    }

    public String getResource() {
        return resource;
    }
}
