package org.paper.exception;

/**
 * Excepción cuando se intenta crear una entidad que ya existe
 */
public class DuplicateEntityException extends RuntimeException {
    private final String entityName;
    private final String field;
    private final Object value;

    public DuplicateEntityException(String entityName, String field, Object value) {
        super(String.format("%s con %s '%s' ya existe", entityName, field, value));
        this.entityName = entityName;
        this.field = field;
        this.value = value;
    }

    public String getEntityName() {
        return entityName;
    }

    public String getField() {
        return field;
    }

    public Object getValue() {
        return value;
    }
}
