package org.paper.dto;

public enum TipoBolsaEnum {
    FONDO_AMERICANO("Bolsa Americana"),
    FONDO_CUADRADO_CON_MANIJA("Bolsa con Asa"),
    FONDO_CUADRADO_SIN_MANIJA("Bolsa Sin Asa"),
    GENERICO("Genérico");

    private final String displayName;

    TipoBolsaEnum(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static TipoBolsaEnum fromNombre(String nombre) {
        for (TipoBolsaEnum tipo : values()) {
            if (tipo.getDisplayName().equalsIgnoreCase(nombre)) {
                return tipo;
            }
        }
        return GENERICO;
    }
}