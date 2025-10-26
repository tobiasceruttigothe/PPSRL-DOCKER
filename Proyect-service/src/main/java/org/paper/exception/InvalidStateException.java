package org.paper.exception;

/**
 * Excepción cuando una entidad está en un estado inválido para la operación
 */
public class InvalidStateException extends RuntimeException {
    private final String entityName;
    private final String currentState;
    private final String operation;

    public InvalidStateException(String entityName, String currentState, String operation) {
        super(String.format("No se puede realizar la operación '%s' sobre %s en estado '%s'",
                operation, entityName, currentState));
        this.entityName = entityName;
        this.currentState = currentState;
        this.operation = operation;
    }

    public String getEntityName() {
        return entityName;
    }

    public String getCurrentState() {
        return currentState;
    }

    public String getOperation() {
        return operation;
    }
}
