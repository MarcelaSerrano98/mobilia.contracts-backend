-- =============================================================================
-- V2 - Datos de ejemplo
-- =============================================================================
-- El juego de datos esta disennado para ejercitar TODAS las reglas del enunciado:
--
--   * property 1  -> 1 contrato ACTIVO + 2 INACTIVOS  (el "historial" del inmueble)
--   * property 5  -> 0 contratos activos, solo historial inactivo
--   * CT-2024-002 -> 2 propietarios                    ("1 o mas")
--   * CT-2023-118 -> 2 deudores solidarios             ("1 o mas si aplica")
--   * CT-2024-001 -> sin deudor solidario              ("no obligatorio")
--   * Carlos Torres es ARRENDATARIO en un contrato y PROPIETARIO en otro
--   * Jorge Restrepo es PROPIETARIO en uno y DEUDOR_SOLIDARIO en otro
--   * Hay tildes en los nombres para comprobar la busqueda insensible a acentos
-- =============================================================================

INSERT INTO person (id, first_name, last_name, document_number, email) VALUES
    (1,  'Juan Carlos',    'Pérez Gómez',      '1020304050', 'juan.perez@example.com'),
    (2,  'María Elena',    'Rodríguez Silva',  '52123456',   'maria.rodriguez@example.com'),
    (3,  'Andrés Felipe',  'Gómez Ruiz',       '79456123',   'andres.gomez@example.com'),
    (4,  'Laura Sofía',    'Martínez Ríos',    '1098765432', 'laura.martinez@example.com'),
    (5,  'Carlos Alberto', 'Torres Vega',      '80112233',   'carlos.torres@example.com'),
    (6,  'Diana Patricia', 'Ospina Cano',      '43998877',   'diana.ospina@example.com'),
    (7,  'Ricardo Andrés', 'Salazar Mesa',     '91445566',   'ricardo.salazar@example.com'),
    (8,  'Valentina',      'Gómez Herrera',    '1144556677', 'valentina.gomez@example.com'),
    (9,  'Jorge Iván',     'Restrepo Ángel',   '70334455',   'jorge.restrepo@example.com'),
    (10, 'Paula Andrea',   'Núñez Bravo',      '1032998877', 'paula.nunez@example.com');


INSERT INTO property (id, address, type) VALUES
    (1, 'Calle 45 # 12-34 Apto 501, Bogotá',        'APARTAMENTO'),
    (2, 'Carrera 70 # 23-15, Medellín',             'CASA'),
    (3, 'Avenida 6N # 28-45 Local 3, Cali',         'LOCAL'),
    (4, 'Transversal 93 # 51-98 Apto 802, Bogotá',  'APARTAMENTO'),
    (5, 'Calle 10 # 5-51, Bucaramanga',             'CASA');


-- OJO: no se inserta la columna generada (active_property_id): la calcula MySQL.
INSERT INTO contract (id, code, status, property_id) VALUES
    (1, 'CT-2024-001', 'ACTIVO',   1),
    (2, 'CT-2022-014', 'INACTIVO', 1),
    (3, 'CT-2020-007', 'INACTIVO', 1),
    (4, 'CT-2024-002', 'ACTIVO',   2),
    (5, 'CT-2023-118', 'ACTIVO',   3),
    (6, 'CT-2025-030', 'ACTIVO',   4),
    (7, 'CT-2021-055', 'INACTIVO', 5);


INSERT INTO contract_party (contract_id, person_id, role) VALUES
    -- CT-2024-001 (activo): 1 arrendatario, 1 propietario, sin deudor solidario
    (1, 1,  'ARRENDATARIO'),
    (1, 2,  'PROPIETARIO'),

    -- CT-2022-014 (inactivo, mismo inmueble): historial del inmueble 1
    (2, 4,  'ARRENDATARIO'),
    (2, 2,  'PROPIETARIO'),

    -- CT-2020-007 (inactivo, mismo inmueble)
    (3, 5,  'ARRENDATARIO'),
    (3, 2,  'PROPIETARIO'),

    -- CT-2024-002: DOS propietarios y un deudor solidario
    (4, 3,  'ARRENDATARIO'),
    (4, 6,  'PROPIETARIO'),
    (4, 9,  'PROPIETARIO'),
    (4, 7,  'DEUDOR_SOLIDARIO'),

    -- CT-2023-118: DOS deudores solidarios
    (5, 8,  'ARRENDATARIO'),
    (5, 5,  'PROPIETARIO'),
    (5, 10, 'DEUDOR_SOLIDARIO'),
    (5, 9,  'DEUDOR_SOLIDARIO'),

    -- CT-2025-030: sin deudor solidario
    (6, 10, 'ARRENDATARIO'),
    (6, 7,  'PROPIETARIO'),

    -- CT-2021-055 (inactivo): el inmueble 5 solo tiene historial
    (7, 6,  'ARRENDATARIO'),
    (7, 4,  'PROPIETARIO'),
    (7, 1,  'DEUDOR_SOLIDARIO');
