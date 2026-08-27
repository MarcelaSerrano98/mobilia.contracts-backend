---
name: verify-db-rules
description: Audita la base de datos MySQL y las entidades JPA de Spring Boot para garantizar el cumplimiento de las reglas de negocio de la prueba técnica de contratos e inmuebles.
disable-model-invocation: false
allowed-tools: Read, Grep, Bash
---

# Auditoría de Reglas de Negocio (MySQL & JPA) — Mobilia Software

## Objetivo

Garantizar, mediante análisis estático del esquema y del código, que el back-end
cumple el modelo relacional y las restricciones que especifica la prueba técnica
de Mobilia Software.

---

## Mapa del código

El proyecto nombra las clases **en inglés**, siguiendo la convención habitual de
Java. Esta es la correspondencia con los términos del enunciado:

| Enunciado | Clase Java | Tabla | Archivo |
|---|---|---|---|
| Inmueble | `Property` | `property` | `src/main/java/com/mobilia/contracts/domain/Property.java` |
| Contrato | `Contract` | `contract` | `src/main/java/com/mobilia/contracts/domain/Contract.java` |
| Persona | `Person` | `person` | `src/main/java/com/mobilia/contracts/domain/Person.java` |
| Parte del contrato | `ContractParty` | `contract_party` | `src/main/java/com/mobilia/contracts/domain/ContractParty.java` |
| Arrendatario / Propietario / Deudor solidario | `PartyRole` | columna `role` | `src/main/java/com/mobilia/contracts/domain/PartyRole.java` |
| Estado del contrato | `ContractStatus` | columna `status` | `src/main/java/com/mobilia/contracts/domain/ContractStatus.java` |
| Tipo de inmueble | `PropertyType` | columna `type` | `src/main/java/com/mobilia/contracts/domain/PropertyType.java` |

Archivos clave fuera del dominio:

- Esquema: `src/main/resources/db/migration/V1__create_schema.sql`
- Datos de ejemplo: `src/main/resources/db/migration/V2__insert_seed_data.sql`
- Configuración JPA: `src/main/resources/application.yml`
- Tests de restricciones: `src/test/java/com/mobilia/contracts/repository/SchemaConstraintsIT.java`

---

## Reglas de negocio obligatorias a auditar

1. **Restricción de contrato activo por inmueble**: un inmueble puede estar
   asociado a un máximo de un (1) contrato en estado `ACTIVO`, y a 0 o más en
   estado `INACTIVO`.

2. **Roles obligatorios en el contrato**:
   - Exactamente un (1) arrendatario (obligatorio).
   - Uno (1) o más propietarios (obligatorio).
   - Cero (0) o más deudores solidarios (opcional).

3. **Mapeo de campos requeridos**:
   - **Inmueble**: dirección y tipo (sólo admite `CASA`, `APARTAMENTO`, `LOCAL`).
   - **Contrato**: código alfanumérico y estado (sólo admite `ACTIVO`, `INACTIVO`).
   - **Persona**: nombre, apellidos, documento de identidad y email.

> Los valores se almacenan en mayúsculas (`CASA`, `ACTIVO`) porque son constantes
> de un `enum` de Java mapeadas con `@Enumerated(EnumType.STRING)`, y las
> constantes en Java se escriben en mayúsculas. Equivalen a los `Casa` y `Activo`
> del enunciado; **no lo reportes como incumplimiento**.

---

## Dónde puede vivir el cumplimiento de una regla

Este es el criterio con el que debes juzgar, y es el punto más importante de
esta skill. Una regla puede imponerse en tres niveles, de más fuerte a más débil:

| Nivel | Alcance | Veredicto |
|---|---|---|
| **Restricción en la base de datos** | Nadie puede saltársela: ni la aplicación, ni un script, ni otro servicio | **CUMPLE** |
| **Validación en la capa de servicio** | Sólo protege frente a los errores de esta aplicación | **CUMPLE** (mejora la experiencia, no sustituye a la anterior) |
| **Sin comprobación** | El dato corrupto entra | **INCUMPLE** |

Una regla garantizada por la base de datos **cumple**, aunque no exista ninguna
validación en Java. No lo reportes como fallo.

### Cardinalidades mínimas: el límite conocido

«Al menos 1 propietario» y «al menos 1 arrendatario» **no son expresables** con
restricciones declarativas en ningún motor relacional: el contrato tendría que
nacer con sus partes en el mismo instante. Es una limitación conocida y
documentada, no un descuido. Sólo repórtala si existe una ruta de escritura que
la deje sin validar (ver instrucción 4).

---

## Instrucciones

### 1. Regla 1 — máximo un contrato activo por inmueble

Comprueba en `V1__create_schema.sql` que la tabla `contract` tenga:

- una columna generada que valga `property_id` sólo si `status = 'ACTIVO'` y
  `NULL` en caso contrario, y
- un índice `UNIQUE` sobre esa columna.

```bash
grep -n -A2 "GENERATED ALWAYS AS" src/main/resources/db/migration/V1__create_schema.sql
grep -n "uk_contract_one_active_per_property" src/main/resources/db/migration/V1__create_schema.sql
```

El mecanismo se apoya en que MySQL **ignora los `NULL` en los índices únicos**:
los contratos inactivos generan `NULL` y nunca colisionan; dos activos del mismo
inmueble generan el mismo valor y el motor rechaza la operación. MySQL no admite
índices únicos parciales, por lo que ésta es la forma correcta de expresarlo.

Verifica también que la columna sea `VIRTUAL` y no `STORED`: MySQL rechaza una
clave foránea con `ON DELETE CASCADE` sobre la columna base de una columna
generada `STORED` (error 1215).

### 2. Regla 2 — cardinalidad de los roles

- **Máximo un arrendatario**: mismo patrón, columna generada `tenant_contract_id`
  más índice `uk_contract_party_single_tenant`.
- **Sin duplicados por rol**: `UNIQUE (contract_id, person_id, role)`.
- **Mínimos**: ver el apartado de cardinalidades mínimas. No es un fallo.

```bash
grep -n "uk_contract_party_single_tenant\|uk_contract_party_unique_role" \
  src/main/resources/db/migration/V1__create_schema.sql
```

### 3. Regla 3 — mapeo de campos y relaciones

Contrasta las entidades JPA con el esquema:

- `Property`: `address` y `type`; `type` con `@Enumerated(EnumType.STRING)` y
  restricción `CHECK` en la tabla.
- `Contract`: `code` (`UNIQUE`) y `status`, también con `CHECK`.
- `Person`: `firstName`, `lastName`, `documentNumber` (`UNIQUE`) y `email`.
- `ContractParty`: `@ManyToOne` a `Contract` y a `Person`, ambos `LAZY`, más el
  `role`.
- `Contract.parties`: `@OneToMany(mappedBy = "contract")`.

Marca como incumplimiento:

- `@Enumerated` con `EnumType.ORDINAL`, o ausente (el valor por defecto **es**
  `ORDINAL`): persiste la posición de la constante, de modo que reordenar el
  `enum` corrompe en silencio los datos ya guardados.
- Un `@ManyToOne` sin `fetch = FetchType.LAZY`: su valor por defecto es `EAGER`.
- Una columna `NOT NULL` en el esquema cuyo campo JPA no declare
  `nullable = false`.

```bash
grep -rn "@Enumerated\|@ManyToOne\|@OneToMany" src/main/java/com/mobilia/contracts/domain/
```

Comprueba además que `application.yml` mantenga
`spring.jpa.hibernate.ddl-auto: validate`. Con `update` o `create`, Hibernate
modificaría el esquema por su cuenta y las restricciones anteriores podrían
desaparecer sin que nadie se entere.

### 4. Rutas de escritura

Localiza cualquier operación que persista datos:

```bash
grep -rn "@PostMapping\|@PutMapping\|@PatchMapping\|\.save(\|\.saveAll(\|@Modifying" \
  src/main/java/
```

- **Si no hay ninguna** (estado actual del proyecto: la API es de sólo lectura,
  expone únicamente `GET /api/v1/contracts/search`), entonces no hay nada que
  validar en la capa de servicio. **No lo reportes como incumplimiento**;
  menciónalo como contexto.

- **Si aparece una ruta de escritura**, entonces sí exige que el servicio
  correspondiente valide *antes* de persistir y lance una excepción controlada
  —traducida a HTTP 409 Conflict en `GlobalExceptionHandler`— en lugar de dejar
  que aflore un `SQLIntegrityConstraintViolationException` crudo. La restricción
  de la base de datos sigue siendo la garantía; la validación en Java aporta un
  mensaje comprensible. Debe cubrir:
  - segundo contrato `ACTIVO` sobre un inmueble que ya tiene uno,
  - contrato sin arrendatario o con más de uno,
  - contrato sin ningún propietario.

### 5. Verificación dinámica (opcional)

Si hay Docker disponible, los tests de integración ya comprueban en SQL nativo
que la base de datos rechaza los estados inválidos:

```bash
./mvnw verify -Dit.test=SchemaConstraintsIT
```

Sin Docker se omiten en lugar de fallar, por
`@Testcontainers(disabledWithoutDocker = true)`.

---

## Formato del reporte

Presenta siempre una tabla resumen y sólo después el detalle:

| Regla | Veredicto | Dónde se impone | Evidencia |
|---|---|---|---|
| 1 · Un contrato activo por inmueble | CUMPLE / INCUMPLE | Base de datos / Servicio / Ninguno | `archivo:línea` |
| 2 · Exactamente 1 arrendatario | … | … | … |
| 2 · Al menos 1 propietario | … | … | … |
| 3 · Campos de Inmueble / Contrato / Persona | … | … | … |

Reglas del reporte:

- Cita siempre `archivo:línea`. Nunca afirmes que algo cumple sin haberlo leído.
- Si todo cumple, dilo con claridad y no inventes hallazgos menores para rellenar.
- Por cada incumplimiento real, muestra el código Java o SQL exacto que lo
  corrige, listo para aplicar, y explica **qué falla hoy** en términos concretos:
  qué dato corrupto podría entrar y por qué camino.
