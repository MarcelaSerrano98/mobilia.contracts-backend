-- =============================================================================
-- V1 - Esquema inicial
-- Prueba tecnica Mobilia Software - historial de inmuebles y contratos
-- =============================================================================
--
-- Convenciones aplicadas:
--   * Tablas y columnas en snake_case y singular.
--   * Claves primarias sustitutas (surrogate keys) BIGINT AUTO_INCREMENT.
--   * Los enumerados se modelan como VARCHAR + CHECK en lugar de ENUM de MySQL:
--       - ENUM es propietario de MySQL y su ALTER es costoso.
--       - VARCHAR + CHECK es portable y encaja con @Enumerated(EnumType.STRING).
--   * Colacion utf8mb4_0900_ai_ci -> las comparaciones ignoran mayusculas y
--     tildes, por lo que buscar "jose" encuentra "José" sin normalizar nada.
-- =============================================================================


-- -----------------------------------------------------------------------------
-- PERSONA
-- Cualquier interviniente de un contrato. Se guarda UNA sola vez aunque
-- participe en varios contratos y con roles distintos.
-- -----------------------------------------------------------------------------
CREATE TABLE person
(
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    first_name      VARCHAR(100) NOT NULL COMMENT 'Nombres',
    last_name       VARCHAR(100) NOT NULL COMMENT 'Apellidos',
    document_number VARCHAR(30)  NOT NULL COMMENT 'Documento de identidad',
    email           VARCHAR(150) NOT NULL,
    created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_person PRIMARY KEY (id),

    -- El documento de identidad identifica de forma unica a una persona:
    -- es la clave natural del negocio y evita registros duplicados.
    CONSTRAINT uk_person_document_number UNIQUE (document_number)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX ix_person_last_name ON person (last_name);
CREATE INDEX ix_person_email ON person (email);


-- -----------------------------------------------------------------------------
-- INMUEBLE
-- -----------------------------------------------------------------------------
CREATE TABLE property
(
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    address    VARCHAR(255) NOT NULL COMMENT 'Direccion del inmueble',
    type       VARCHAR(20)  NOT NULL COMMENT 'CASA | APARTAMENTO | LOCAL',
    created_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_property PRIMARY KEY (id),

    -- El CHECK impide que llegue a la tabla un tipo que la aplicacion no conoce,
    -- aunque el INSERT venga por fuera de la aplicacion (script, consola, etc.).
    CONSTRAINT ck_property_type CHECK (type IN ('CASA', 'APARTAMENTO', 'LOCAL'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX ix_property_address ON property (address);


-- -----------------------------------------------------------------------------
-- CONTRATO
--
-- Regla del enunciado:
--   "Un inmueble esta asociado a 1 contrato activo (maximo un contrato activo),
--    y 0 o mas contratos inactivos"
--
-- Como se garantiza esa regla EN LA BASE DE DATOS:
--   MySQL no soporta indices unicos parciales (el 'CREATE UNIQUE INDEX ...
--   WHERE status = ...' de PostgreSQL). La solucion es una COLUMNA GENERADA:
--
--     active_property_id = property_id   si el contrato esta ACTIVO
--     active_property_id = NULL          si esta INACTIVO
--
--   y un indice UNIQUE sobre ella. Como MySQL NO considera los NULL a efectos
--   de unicidad, los contratos inactivos nunca colisionan entre si, mientras
--   que dos contratos ACTIVOS del mismo inmueble producirian el mismo valor y
--   el motor rechaza el INSERT. La regla queda blindada a nivel de motor.
--
--   Se declara VIRTUAL y no STORED: el valor no ocupa espacio en la fila, se
--   materializa unicamente dentro del indice. Ademas MySQL prohibe que una
--   columna base de una columna generada STORED participe en una clave foranea
--   con ON DELETE CASCADE, restriccion que VIRTUAL no tiene.
-- -----------------------------------------------------------------------------
CREATE TABLE contract
(
    id                 BIGINT      NOT NULL AUTO_INCREMENT,
    code               VARCHAR(30) NOT NULL COMMENT 'Codigo alfanumerico del contrato',
    status             VARCHAR(20) NOT NULL COMMENT 'ACTIVO | INACTIVO',
    property_id        BIGINT      NOT NULL,
    created_at         DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at         DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    active_property_id BIGINT GENERATED ALWAYS AS
        (CASE WHEN status = 'ACTIVO' THEN property_id END) VIRTUAL,

    CONSTRAINT pk_contract PRIMARY KEY (id),
    CONSTRAINT uk_contract_code UNIQUE (code),
    CONSTRAINT ck_contract_status CHECK (status IN ('ACTIVO', 'INACTIVO')),

    -- Maximo UN contrato activo por inmueble.
    CONSTRAINT uk_contract_one_active_per_property UNIQUE (active_property_id),

    CONSTRAINT fk_contract_property FOREIGN KEY (property_id)
        REFERENCES property (id)
        ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX ix_contract_property_id ON contract (property_id);


-- -----------------------------------------------------------------------------
-- PARTE DEL CONTRATO  (tabla de union con atributo)
--
-- Por que una tabla aparte y no tres columnas en 'contract':
--   El enunciado admite 1..N propietarios y 0..N deudores solidarios. Modelarlo
--   con columnas (owner_1_id, owner_2_id, ...) seria un grupo repetitivo, que
--   viola la Primera Forma Normal y limita artificialmente la cardinalidad.
--   'contract_party' resuelve una relacion M:N entre contrato y persona,
--   cualificada por el rol que esa persona ejerce en ese contrato.
--
-- Regla del enunciado:
--   "Arrendatario / Inquilino -> Si, puede haber solo 1"
--   Se aplica el mismo patron de columna generada VIRTUAL + UNIQUE que en
--   'contract'. Aqui VIRTUAL es obligatorio: contract_id es la columna base de
--   la columna generada y a la vez la clave foranea con ON DELETE CASCADE, algo
--   que MySQL rechaza si la columna generada es STORED (ERROR 1215).
-- -----------------------------------------------------------------------------
CREATE TABLE contract_party
(
    id                 BIGINT      NOT NULL AUTO_INCREMENT,
    contract_id        BIGINT      NOT NULL,
    person_id          BIGINT      NOT NULL,
    role               VARCHAR(30) NOT NULL COMMENT 'ARRENDATARIO | PROPIETARIO | DEUDOR_SOLIDARIO',
    created_at         DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    tenant_contract_id BIGINT GENERATED ALWAYS AS
        (CASE WHEN role = 'ARRENDATARIO' THEN contract_id END) VIRTUAL,

    CONSTRAINT pk_contract_party PRIMARY KEY (id),

    CONSTRAINT ck_contract_party_role
        CHECK (role IN ('ARRENDATARIO', 'PROPIETARIO', 'DEUDOR_SOLIDARIO')),

    -- Una persona no puede figurar dos veces con el mismo rol en el mismo
    -- contrato (si puede ser, por ejemplo, propietaria y deudora solidaria).
    CONSTRAINT uk_contract_party_unique_role
        UNIQUE (contract_id, person_id, role),

    -- Maximo UN arrendatario por contrato.
    CONSTRAINT uk_contract_party_single_tenant UNIQUE (tenant_contract_id),

    CONSTRAINT fk_contract_party_contract FOREIGN KEY (contract_id)
        REFERENCES contract (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_contract_party_person FOREIGN KEY (person_id)
        REFERENCES person (id)
        ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX ix_contract_party_person_id ON contract_party (person_id);
CREATE INDEX ix_contract_party_contract_role ON contract_party (contract_id, role);

-- NOTA sobre el rendimiento de la busqueda:
-- La consulta usa LIKE '%texto%'. Un comodin a la izquierda impide que MySQL
-- aproveche un indice B-Tree, por lo que estos indices sirven a los JOIN y a
-- las busquedas exactas, no al filtro por contenido. Para un volumen alto el
-- siguiente paso natural seria un indice FULLTEXT (MATCH ... AGAINST) o un
-- motor de busqueda externo; para el volumen de esta prueba, LIKE es suficiente
-- y mantiene la semantica exacta que pide el enunciado ("que contenga el texto").
