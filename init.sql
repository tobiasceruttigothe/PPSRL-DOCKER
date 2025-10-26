-- init.sql
-- Base de datos para el sistema Paper SRL

-- ============================================
-- TABLA: usuarios
-- ============================================
-- Tabla que sincroniza con Keycloak
-- Solo almacena el UUID, estado y metadata de sincronización
CREATE TABLE IF NOT EXISTS usuarios (
    id UUID PRIMARY KEY,
    fecha_registro TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    
    CONSTRAINT chk_usuarios_status CHECK (status IN ('PENDING', 'ACTIVE', 'FAILED'))
);

-- Índices para mejorar performance
CREATE INDEX IF NOT EXISTS idx_usuarios_status ON usuarios(status);
CREATE INDEX IF NOT EXISTS idx_usuarios_fecha_registro ON usuarios(fecha_registro);

-- ============================================
-- TABLA: materiales
-- ============================================
-- Catálogo de materiales disponibles para bolsas
CREATE TABLE IF NOT EXISTS materiales (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE
);

-- Índice
CREATE INDEX IF NOT EXISTS idx_materiales_nombre ON materiales(nombre);

-- ============================================
-- TABLA: tipos_bolsa
-- ============================================
-- Catálogo de tipos de bolsa disponibles
CREATE TABLE IF NOT EXISTS tipos_bolsa (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE
);

-- Índice
CREATE INDEX IF NOT EXISTS idx_tipos_bolsa_nombre ON tipos_bolsa(nombre);

-- ============================================
-- TABLA: plantillas
-- ============================================
-- Plantillas base para crear diseños
CREATE TABLE IF NOT EXISTS plantillas (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    material_id INTEGER NOT NULL,
    tipo_bolsa_id INTEGER NOT NULL,
    base64_plantilla TEXT NOT NULL,
    ancho REAL NOT NULL,
    alto REAL NOT NULL,
    profundidad REAL NOT NULL,
    
    CONSTRAINT fk_plantillas_material 
        FOREIGN KEY (material_id) 
        REFERENCES materiales(id) 
        ON DELETE RESTRICT,
    
    CONSTRAINT fk_plantillas_tipo_bolsa 
        FOREIGN KEY (tipo_bolsa_id) 
        REFERENCES tipos_bolsa(id) 
        ON DELETE RESTRICT,
    
    CONSTRAINT chk_plantillas_dimensiones 
        CHECK (ancho > 0 AND alto > 0 AND profundidad >= 0)
);

-- Índices
CREATE INDEX IF NOT EXISTS idx_plantillas_material_id ON plantillas(material_id);
CREATE INDEX IF NOT EXISTS idx_plantillas_tipo_bolsa_id ON plantillas(tipo_bolsa_id);
CREATE INDEX IF NOT EXISTS idx_plantillas_nombre ON plantillas(nombre);

-- ============================================
-- TABLA: logos
-- ============================================
-- Logos personalizados de los usuarios
CREATE TABLE IF NOT EXISTS logos (
    id SERIAL PRIMARY KEY,
    usuario_id UUID NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    base64_logo TEXT NOT NULL,
    fecha_creacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    tamano_bytes BIGINT,
    
    CONSTRAINT fk_logos_usuario 
        FOREIGN KEY (usuario_id) 
        REFERENCES usuarios(id) 
        ON DELETE CASCADE
);

-- Índices
CREATE INDEX IF NOT EXISTS idx_logos_usuario_id ON logos(usuario_id);
CREATE INDEX IF NOT EXISTS idx_logos_fecha_creacion ON logos(fecha_creacion);

-- ============================================
-- TABLA: disenos
-- ============================================
-- Diseños creados por los usuarios basados en plantillas
CREATE TABLE IF NOT EXISTS disenos (
    id SERIAL PRIMARY KEY,
    usuario_id UUID NOT NULL,
    plantilla_id INTEGER NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    descripcion TEXT,
    base64_diseno TEXT NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PROGRESO',
    fecha_creacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    fecha_actualizacion TIMESTAMP WITH TIME ZONE,
    
    CONSTRAINT fk_disenos_usuario 
        FOREIGN KEY (usuario_id) 
        REFERENCES usuarios(id) 
        ON DELETE CASCADE,
    
    CONSTRAINT fk_disenos_plantilla 
        FOREIGN KEY (plantilla_id) 
        REFERENCES plantillas(id) 
        ON DELETE RESTRICT,
    
    CONSTRAINT chk_disenos_estado 
        CHECK (estado IN ('PROGRESO', 'TERMINADO'))
);

-- Índices
CREATE INDEX IF NOT EXISTS idx_disenos_usuario_id ON disenos(usuario_id);
CREATE INDEX IF NOT EXISTS idx_disenos_plantilla_id ON disenos(plantilla_id);
CREATE INDEX IF NOT EXISTS idx_disenos_estado ON disenos(estado);
CREATE INDEX IF NOT EXISTS idx_disenos_fecha_creacion ON disenos(fecha_creacion);

-- ============================================
-- TABLA: usuario_plantilla (ManyToMany)
-- ============================================
-- Relación entre usuarios y plantillas habilitadas para ellos
CREATE TABLE IF NOT EXISTS usuario_plantilla (
    usuario_id UUID NOT NULL,
    plantilla_id INTEGER NOT NULL,
    
    PRIMARY KEY (usuario_id, plantilla_id),
    
    CONSTRAINT fk_usuario_plantilla_usuario 
        FOREIGN KEY (usuario_id) 
        REFERENCES usuarios(id) 
        ON DELETE CASCADE,
    
    CONSTRAINT fk_usuario_plantilla_plantilla 
        FOREIGN KEY (plantilla_id) 
        REFERENCES plantillas(id) 
        ON DELETE CASCADE
);

-- Índices
CREATE INDEX IF NOT EXISTS idx_usuario_plantilla_usuario_id ON usuario_plantilla(usuario_id);
CREATE INDEX IF NOT EXISTS idx_usuario_plantilla_plantilla_id ON usuario_plantilla(plantilla_id);

-- ============================================
-- DATOS INICIALES (Catálogos)
-- ============================================

-- Materiales iniciales
INSERT INTO materiales (nombre) 
VALUES 
    ('Papel Kraft'),
    ('Papel Blanco'),
    ('Papel Reciclado'),
    ('Cartón Corrugado'),
    ('Polipropileno')
ON CONFLICT (nombre) DO NOTHING;

-- Tipos de bolsa iniciales
INSERT INTO tipos_bolsa (nombre) 
VALUES 
    ('Bolsa con Asa'),
    ('Bolsa Sin Asa'),
    ('Bolsa Americana'),
    ('Bolsa Camiseta'),
    ('Bolsa Boutique')
ON CONFLICT (nombre) DO NOTHING;

-- ============================================
-- INFORMACIÓN DE LA BASE DE DATOS
-- ============================================
-- Versión del schema
CREATE TABLE IF NOT EXISTS schema_version (
    version INTEGER PRIMARY KEY,
    applied_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    description TEXT
);

INSERT INTO schema_version (version, description) 
VALUES (2, 'Schema completo con usuarios, materiales, tipos_bolsa, plantillas, logos, disenos y relaciones ManyToMany')
ON CONFLICT (version) DO UPDATE SET 
    applied_at = EXCLUDED.applied_at,
    description = EXCLUDED.description;

-- ============================================
-- COMENTARIOS EN LAS TABLAS (Documentación)
-- ============================================

COMMENT ON TABLE usuarios IS 'Tabla que sincroniza con Keycloak. Solo almacena UUID, estado y metadata.';
COMMENT ON COLUMN usuarios.id IS 'UUID del usuario en Keycloak';
COMMENT ON COLUMN usuarios.status IS 'Estado del usuario: PENDING (creado pero no verificado), ACTIVE (verificado y activo), FAILED (fallo en sincronización)';

COMMENT ON TABLE materiales IS 'Catálogo de materiales disponibles para bolsas';
COMMENT ON TABLE tipos_bolsa IS 'Catálogo de tipos de bolsa disponibles';
COMMENT ON TABLE plantillas IS 'Plantillas base para crear diseños de bolsas';
COMMENT ON TABLE logos IS 'Logos personalizados subidos por los usuarios';
COMMENT ON TABLE disenos IS 'Diseños creados por usuarios basados en plantillas';
COMMENT ON TABLE usuario_plantilla IS 'Relación ManyToMany entre usuarios y plantillas habilitadas';

COMMENT ON COLUMN disenos.estado IS 'Estado del diseño: PROGRESO (en edición), TERMINADO (finalizado y listo)';
COMMENT ON COLUMN plantillas.base64_plantilla IS 'JSON con la imagen de la plantilla en formato base64';
COMMENT ON COLUMN logos.base64_logo IS 'JSON con la imagen del logo en formato base64';
COMMENT ON COLUMN disenos.base64_diseno IS 'JSON con la imagen del diseño en formato base64';
