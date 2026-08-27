# Mobilia · API de contratos e inmuebles

Servicio REST que permite consultar el historial de inmuebles y las partes
asociadas a cada contrato de arrendamiento, buscando por un único texto libre.

Prueba técnica de desarrollo para **Mobilia Software**.

| | |
|---|---|
| **Repositorio back-end** | https://github.com/MarcelaSerrano98/mobilia.contracts-backend |
| **Repositorio front-end** | https://github.com/MarcelaSerrano98/mobilia.contracts-frontend |
| **Documentación de la API** | Swagger UI en `/swagger-ui.html` — **es una URL local**: sólo responde con la aplicación en marcha, no es un enlace público |

---

## Tabla de contenido

1. [Qué resuelve](#qué-resuelve)
2. [Stack tecnológico](#stack-tecnológico)
3. [Requisitos previos](#requisitos-previos)
4. [Cómo ejecutarlo](#cómo-ejecutarlo)
5. [Configuración](#configuración)
6. [Resolución de problemas](#resolución-de-problemas)
7. [Modelo de datos](#modelo-de-datos)
8. [La API](#la-api)
9. [Tests](#tests)
10. [Decisiones técnicas](#decisiones-técnicas)
11. [Estructura del proyecto](#estructura-del-proyecto)

---

## Qué resuelve

Se introduce un texto en un único campo de búsqueda y el servicio devuelve
**todos los contratos** en los que ese texto aparece en cualquiera de estos
campos:

- Nombres, apellidos, documento de identidad o email de **cualquiera** de las
  personas del contrato (arrendatario, propietarios o deudores solidarios).
- Dirección del inmueble.
- Código del contrato.

De cada contrato encontrado se devuelve la fila completa: código, dirección,
arrendatario, propietarios y deudores solidarios — **incluidas las partes que no
coinciden con el texto buscado**.

La comparación **ignora mayúsculas y tildes**: buscar `nunez` encuentra `Núñez`.

---

## Stack tecnológico

| Componente | Tecnología | Versión |
|---|---|---|
| Lenguaje | Java | 21 (LTS) |
| Framework | Spring Boot | 3.5.9 |
| Persistencia | Spring Data JPA / Hibernate | 6.x |
| Base de datos | MySQL | 8.4 |
| Migraciones | Flyway | 11.x |
| Documentación | springdoc-openapi (Swagger UI) | 2.8.13 |
| Tests | JUnit 5, Mockito, AssertJ, Testcontainers | — |
| Build | Maven (con wrapper incluido) | 3.9.x |

---

## Requisitos previos

- **JDK 21 o superior.** Comprobar con `java -version`.
- **Una de estas dos opciones para la base de datos:**
  - Docker y Docker Compose _(recomendado, no requiere instalar MySQL)_, o
  - Una instancia local de MySQL 8.x.

No hace falta instalar Maven: el repositorio incluye el *Maven Wrapper*
(`./mvnw`).

---

## Cómo ejecutarlo

### Opción A — con Docker _(recomendada)_

```bash
# 1. Clonar
git clone https://github.com/MarcelaSerrano98/mobilia.contracts-backend.git
cd mobilia.contracts-backend

# 2. Levantar MySQL
docker compose up -d

# 3. Levantar la aplicación
./mvnw spring-boot:run
```

Listo. La API queda en `http://localhost:8080`.

> **¿El paso 2 falla con `address already in use`?** Es que ya tienes un MySQL
> instalado ocupando el puerto 3306. No hace falta desinstalarlo ni detenerlo:
> exporta `DB_PORT` una sola vez y repite los dos pasos.
>
> ```bash
> export DB_PORT=3307
> docker compose up -d
> ./mvnw spring-boot:run
> ```
>
> `DB_PORT` configura **a la vez** el puerto en el que Docker publica MySQL y el
> puerto al que se conecta la aplicación, de modo que no pueden quedar
> desincronizados. Más detalle en
> [Resolución de problemas](#resolución-de-problemas).

> **No hay que ejecutar ningún script SQL a mano.** Flyway crea el esquema y
> carga los datos de ejemplo automáticamente en el primer arranque. El estado de
> las migraciones queda registrado en la tabla `flyway_schema_history`.

### Opción B — con un MySQL ya instalado

1. Crear la base de datos y el usuario:

   ```sql
   CREATE DATABASE mobilia_contracts
     CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

   CREATE USER 'mobilia'@'localhost' IDENTIFIED BY 'mobilia';
   GRANT ALL PRIVILEGES ON mobilia_contracts.* TO 'mobilia'@'localhost';
   FLUSH PRIVILEGES;
   ```

   > La colación `utf8mb4_0900_ai_ci` no es opcional: es la que hace que la
   > búsqueda ignore mayúsculas y tildes.

2. Levantar la aplicación:

   ```bash
   ./mvnw spring-boot:run
   ```

   Si las credenciales son otras, se pasan por variables de entorno:

   ```bash
   DB_HOST=localhost DB_PORT=3306 DB_NAME=mobilia_contracts \
   DB_USER=mi_usuario DB_PASSWORD=mi_clave \
   ./mvnw spring-boot:run
   ```

### Desde un IDE

Importar como **proyecto Maven existente** y ejecutar la clase
`com.mobilia.contracts.ContractsBackendApplication`. No se requiere ninguna
configuración adicional: el `.gitignore` excluye los archivos propios de
IntelliJ IDEA, Eclipse/STS, NetBeans y VS Code, de modo que el proyecto se abre
limpio en cualquiera de ellos.

### Comprobar que funciona

Con la aplicación **ya levantada** (el paso anterior deja la consola ocupada;
abre otra terminal):

```bash
curl "http://localhost:8080/api/v1/contracts/search?q=Gomez"
```

Debe devolver 4 contratos. También puedes explorar la API desde el navegador en
**Swagger UI**:

```
http://localhost:8080/swagger-ui.html
```

> Esa dirección **no es un enlace público**: apunta a tu propia máquina y sólo
> responde mientras la aplicación esté ejecutándose. Si el navegador dice
> «no se puede acceder a este sitio» o «connection refused», casi siempre
> significa que el proceso se detuvo. Compruébalo así:
>
> ```bash
> curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/v3/api-docs
> ```
>
> `200` = todo bien. `000` = la aplicación no está corriendo: vuelve a lanzar
> `./mvnw spring-boot:run`.

---

## Configuración

Toda la configuración vive en `src/main/resources/application.yml` y admite
sobrescritura por variable de entorno, sin tocar el código:

| Variable | Valor por defecto | Descripción |
|---|---|---|
| `DB_HOST` | `localhost` | Host de MySQL |
| `DB_PORT` | `3306` | Puerto de MySQL. Lo leen **tanto** `docker compose` (puerto en el que publica el contenedor) **como** la aplicación (puerto al que se conecta) |
| `DB_NAME` | `mobilia_contracts` | Nombre de la base de datos |
| `DB_USER` | `mobilia` | Usuario |
| `DB_PASSWORD` | `mobilia` | Contraseña |
| `SERVER_PORT` | `8080` | Puerto de la aplicación |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:3000` | Orígenes autorizados del front-end |
| `LOG_LEVEL` | `INFO` | Nivel de log de la aplicación |

---

## Resolución de problemas

### `bind: address already in use` al levantar Docker

```
Error response from daemon: ports are not available: exposing port TCP
0.0.0.0:3306 -> 127.0.0.1:0: listen tcp 0.0.0.0:3306: bind: address already in use
```

Significa que **ya hay un MySQL escuchando en el puerto 3306** de tu máquina,
normalmente uno instalado con Homebrew, `apt` o el instalador oficial. Dos
procesos no pueden ocupar el mismo puerto, así que Docker no puede publicar el
suyo.

Comprobar quién lo tiene ocupado:

```bash
lsof -nP -iTCP:3306 -sTCP:LISTEN     # macOS y Linux
```

Hay tres salidas, en orden de menos a más invasiva:

**1. Usar el MySQL que ya tienes** — no necesitas Docker en absoluto. Sigue la
[Opción B](#opción-b--con-un-mysql-ya-instalado).

**2. Publicar el contenedor en otro puerto** — conviven sin tocarse:

```bash
export DB_PORT=3307
docker compose up -d
./mvnw spring-boot:run
```

Una sola variable configura los dos lados: el puerto del **host** en el que
Docker publica el contenedor, y el puerto al que se conecta la aplicación.
Dentro del contenedor MySQL sigue escuchando en el 3306 siempre; lo único que
cambia es el puerto publicado hacia fuera.

> Con dos variables separadas —una para Docker y otra para la aplicación— era
> posible publicar el contenedor en un puerto y conectar la aplicación a otro.
> En una máquina con MySQL instalado el fallo ni siquiera saltaba: la
> aplicación acababa hablando con la base local en el 3306 en lugar de con el
> contenedor, sin ningún aviso. Reutilizar `DB_PORT` hace ese error imposible.

**3. Detener el MySQL local** — sólo si prefieres no tener dos:

```bash
brew services stop mysql      # macOS con Homebrew
sudo systemctl stop mysql     # Linux con systemd
```

### Swagger UI no carga

`http://localhost:8080/swagger-ui.html` es una dirección **local**: sólo existe
mientras la aplicación esté ejecutándose en tu máquina. No es un enlace que
funcione desde GitHub ni desde otro equipo.

```bash
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/v3/api-docs
```

- `200` → la documentación está disponible; abre la URL en el navegador.
- `000` → la aplicación no está corriendo. Lánzala con `./mvnw spring-boot:run`.

La ruta `/swagger-ui.html` responde con una redirección **302** hacia
`/swagger-ui/index.html`. Es el comportamiento normal de springdoc, no un error.

### `Schema-validation: missing table` al arrancar

Hibernate arranca con `ddl-auto: validate`, es decir, **no crea ni modifica
tablas**: sólo comprueba que las entidades coincidan con el esquema. Si aparece
este error, es que Flyway no llegó a ejecutarse — casi siempre porque la
aplicación se conectó a una base de datos distinta de la que esperabas.
Verifica `DB_NAME`, `DB_HOST` y `DB_PORT`.

### La búsqueda no encuentra nombres con tilde

La insensibilidad a mayúsculas y tildes la aporta la colación
`utf8mb4_0900_ai_ci`. Si creaste la base a mano sin declararla, MySQL habrá
usado la colación por defecto del servidor. Comprobarlo:

```sql
SELECT DEFAULT_COLLATION_NAME FROM information_schema.SCHEMATA
WHERE SCHEMA_NAME = 'mobilia_contracts';
```

Las tablas la declaran de forma explícita en la migración `V1`, así que basta
con borrar la base y dejar que Flyway la recree.

### Los tests de integración no se ejecutan

Es el comportamiento esperado si no hay Docker: se **omiten** en lugar de
fallar, gracias a `@Testcontainers(disabledWithoutDocker = true)`. Con Docker
levantado, `./mvnw verify` los ejecuta.

---

## Modelo de datos

```
┌──────────────────┐         ┌──────────────────────┐         ┌──────────────────┐
│     property     │         │       contract       │         │      person      │
├──────────────────┤         ├──────────────────────┤         ├──────────────────┤
│ id          PK   │1       *│ id              PK   │         │ id          PK   │
│ address          ├─────────┤ code         UNIQUE  │         │ first_name       │
│ type             │         │ status               │         │ last_name        │
│  CASA            │         │  ACTIVO | INACTIVO   │         │ document_number  │
│  APARTAMENTO     │         │ property_id     FK   │         │        UNIQUE    │
│  LOCAL           │         │ ─────────────────    │         │ email            │
└──────────────────┘         │ active_property_id   │         └────────┬─────────┘
                             │  (columna generada)  │                  │1
                             └──────────┬───────────┘                  │
                                        │1                             │
                                        │      ┌────────────────────┐  │
                                        │     *│   contract_party   │* │
                                        └──────┤                    ├──┘
                                               ├────────────────────┤
                                               │ id            PK   │
                                               │ contract_id   FK   │
                                               │ person_id     FK   │
                                               │ role               │
                                               │  ARRENDATARIO      │
                                               │  PROPIETARIO       │
                                               │  DEUDOR_SOLIDARIO  │
                                               │ ────────────────   │
                                               │ tenant_contract_id │
                                               │  (columna generada)│
                                               └────────────────────┘
```

El script completo está en
[`V1__create_schema.sql`](src/main/resources/db/migration/V1__create_schema.sql),
ampliamente comentado.

### Por qué `contract_party` y no columnas en `contract`

El enunciado admite **1 o más propietarios** y **0 o más deudores solidarios**.
Modelarlo con columnas (`propietario_1_id`, `propietario_2_id`, …) sería un
*grupo repetitivo*: incumple la Primera Forma Normal y pone un techo artificial
al número de partes.

`contract_party` resuelve una relación **muchos-a-muchos entre contrato y
persona, cualificada por un atributo** (el rol). Como efecto secundario, una
misma persona puede ser propietaria en un contrato e inquilina en otro sin
duplicar su registro.

### Reglas de negocio garantizadas por la base de datos

| Regla del enunciado | Cómo se garantiza |
|---|---|
| Máximo **1 contrato activo** por inmueble | Columna generada + índice `UNIQUE` |
| Máximo **1 arrendatario** por contrato | Columna generada + índice `UNIQUE` |
| Una persona no se repite con el mismo rol | `UNIQUE (contract_id, person_id, role)` |
| Tipo de inmueble válido | `CHECK (type IN (…))` |
| Estado de contrato válido | `CHECK (status IN (…))` |
| Documento de identidad único | `UNIQUE (document_number)` |

**El truco de la columna generada.** MySQL no soporta índices únicos parciales
(el `CREATE UNIQUE INDEX … WHERE …` de PostgreSQL). La solución es una columna
calculada por el motor:

```sql
active_property_id BIGINT GENERATED ALWAYS AS
    (CASE WHEN status = 'ACTIVO' THEN property_id END) VIRTUAL,
UNIQUE KEY uk_contract_one_active_per_property (active_property_id)
```

Se apoya en que **MySQL ignora los `NULL` en los índices únicos**: los contratos
inactivos generan `NULL` y nunca colisionan; dos contratos activos del mismo
inmueble generan el mismo valor y el motor rechaza la operación.

Se declara `VIRTUAL` y no `STORED` por dos motivos: no ocupa espacio en la fila
(sólo se materializa dentro del índice), y MySQL **rechaza** una clave foránea
con `ON DELETE CASCADE` sobre la columna base de una columna generada `STORED`
(error 1215).

### Lo que la base de datos *no* puede garantizar

Las cardinalidades **mínimas** — «al menos 1 propietario», «al menos 1
arrendatario» — no son expresables con restricciones declarativas en ningún
motor relacional. La razón es de orden: al insertar el contrato todavía no
existe ninguna parte que apunte a él, de modo que cualquier comprobación en ese
instante fallaría siempre.

Un **máximo** sí se expresa —basta con impedir la segunda fila—, y por eso
«máximo 1 contrato activo» y «máximo 1 arrendatario» sí viven en el esquema.

Dos alternativas consideradas y descartadas:

| Alternativa | Por qué no |
|---|---|
| Disparadores `BEFORE DELETE` sobre `contract_party` | Cubren el borrado de la última parte, pero no la inserción. Añaden lógica invisible desde Java, y MySQL no los activa en los borrados en cascada de una clave foránea, lo que produce un comportamiento difícil de anticipar |
| Validación en la capa de servicio | Es la solución correcta, pero requiere una ruta de escritura. Esta API es de sólo lectura, así que hoy no hay dónde colocarla |

Mientras no exista esa ruta de escritura, la garantía es
[`ContractInvariantsIT`](src/test/java/com/mobilia/contracts/repository/ContractInvariantsIT.java):
comprueba en cada `mvn verify`, sobre la totalidad de los datos, que todo
contrato tiene exactamente un arrendatario, al menos un propietario y al menos
dos personas distintas. No impide que un dato inválido entre, pero impide que
pase desapercibido.

### Datos de ejemplo

La migración `V2` carga un juego de datos que ejercita todas las cardinalidades
del enunciado: un inmueble con 1 contrato activo y 2 inactivos (el «historial»),
un contrato con 2 propietarios, otro con 2 deudores solidarios, otro sin ninguno,
personas que cambian de rol entre contratos, y nombres con tildes.

---

## La API

### `GET /api/v1/contracts/search`

| Parámetro | Tipo | Obligatorio | Por defecto | Descripción |
|---|---|---|---|---|
| `q` | string | **Sí** | — | Texto a buscar (mínimo 2 caracteres) |
| `page` | int | No | `0` | Índice de página |
| `size` | int | No | `20` | Resultados por página (máximo 100) |

**Ejemplo:**

```bash
curl "http://localhost:8080/api/v1/contracts/search?q=Gomez&page=0&size=20"
```

```json
{
  "content": [
    {
      "contractCode": "CT-2024-001",
      "contractStatus": "ACTIVO",
      "propertyAddress": "Calle 45 # 12-34 Apto 501, Bogotá",
      "propertyType": "APARTAMENTO",
      "tenant": {
        "fullName": "Juan Carlos Pérez Gómez",
        "documentNumber": "1020304050"
      },
      "owners": [
        { "fullName": "María Elena Rodríguez Silva", "documentNumber": "52123456" }
      ],
      "guarantors": []
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}
```

**Respuesta de error** (formato uniforme para cualquier fallo):

```json
{
  "timestamp": "2026-08-26T13:44:36.950-05:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Falta el parametro obligatorio 'q'.",
  "path": "/api/v1/contracts/search",
  "details": []
}
```

---

## Tests

```bash
./mvnw test      # unitarios y de capa web (rápidos, sin dependencias externas)
./mvnw verify    # además los de integración contra un MySQL real
```

| Tipo | Clase | Qué comprueba |
|---|---|---|
| Unitario | `ContractSearchServiceTest` | Validación del texto, escapado de comodines, límites de paginación y agrupación por rol |
| Capa web | `ContractControllerTest` | Códigos HTTP, forma del JSON y respuestas de error |
| Integración | `ContractRepositoryIT` | La búsqueda sobre un MySQL real: todos los campos, tildes, paginación y **ausencia de N+1** |
| Integración | `SchemaConstraintsIT` | Que la **base de datos** rechaza los estados inválidos, no sólo el código Java |
| Integración | `ContractInvariantsIT` | Las cardinalidades **mínimas** del enunciado, que ninguna restricción declarativa puede expresar |
| Integración | `ContractsBackendApplicationTests` | Que Flyway migra y que Hibernate valida las entidades contra el esquema |

Los tests de integración levantan **MySQL 8.4 en un contenedor efímero**
(Testcontainers) en lugar de usar H2 en modo compatible: el esquema depende de
características que sólo existen en MySQL, y probar sobre H2 validaría un
esquema distinto del que se despliega.

> Si no hay Docker disponible, los tests de integración se **omiten** en lugar de
> fallar (`@Testcontainers(disabledWithoutDocker = true)`), de modo que
> `./mvnw test` sigue siendo verde en cualquier máquina.

---

## Decisiones técnicas

### Por qué la búsqueda usa dos consultas y no una

Es la decisión menos obvia del proyecto. Si se busca `"Gómez"` y esa persona es
**propietaria**, el contrato debe aparecer, pero la fila tiene que mostrar
**también** al arrendatario y a los deudores solidarios, que no contienen el
texto. Filtrar y proyectar en la misma consulta devolvería únicamente las partes
coincidentes — un error silencioso y difícil de detectar.

Además, combinar `JOIN FETCH` de una colección con paginación obliga a Hibernate
a traer todas las filas y paginar **en memoria** (aviso `HHH90003004`).

Separar en dos consultas evita las dos trampas:

1. **Qué contratos coinciden** → filtra y pagina sobre identificadores.
2. **Cargar esos contratos completos** → una única consulta con `JOIN FETCH` de
   inmueble, partes y personas, sin problema N+1.

`ContractRepositoryIT#loadsEverythingInASingleQuery` verifica con las
estadísticas de Hibernate que recorrer el grafo completo no dispara ni una
consulta adicional.

### Por qué `EXISTS` y no un `JOIN` sobre las partes

Un `JOIN` con las partes multiplica filas: un contrato con tres partes
coincidentes aparecería tres veces y obligaría a un `DISTINCT`, que a su vez
impide ordenar por una columna ausente de la proyección (MySQL, error 3065). Con
`EXISTS` cada contrato aparece una sola vez y la paginación cuenta lo correcto.

### Por qué no se usa `LOWER()` en la consulta

La insensibilidad a mayúsculas **y a tildes** la aporta la colación
`utf8mb4_0900_ai_ci`. `LOWER()` daría una falsa sensación de portabilidad,
porque no resuelve las tildes en ningún motor.

### Escapado de comodines

El texto de la persona se escapa antes de construir el patrón `LIKE`: sin ello,
teclear `%` devolvería todos los contratos y `_` actuaría como comodín. No es un
riesgo de inyección SQL (el valor viaja como parámetro enlazado), pero sí un
resultado que sorprende a quien busca.

### Flyway en lugar de `ddl-auto: update`

El esquema es un artefacto versionado, revisable y reproducible. `ddl-auto` está
en **`validate`**: Hibernate no crea ni modifica nada, sólo comprueba que las
entidades coincidan con el esquema — y falla al arrancar si no es así.

### `open-in-view: false`

Desactivado a propósito. El valor por defecto de Spring Boot mantiene la sesión
de Hibernate abierta durante el renderizado de la respuesta, lo que oculta los
problemas de *fetching* hasta que aparecen en producción. Desactivarlo obliga a
resolverlos en la capa de servicio, donde deben resolverse.

### DTOs en lugar de exponer las entidades

No se filtra al cliente el modelo interno ni las columnas de auditoría, Jackson
nunca recorre asociaciones perezosas fuera de la transacción, y el contrato de la
API queda desacoplado del esquema.

### Enumerados como `VARCHAR` + `CHECK`

En lugar del tipo `ENUM` de MySQL, que es propietario y cuyo `ALTER` es costoso.
`VARCHAR` + `CHECK` es portable y encaja de forma natural con
`@Enumerated(EnumType.STRING)`.

### Sobre el rendimiento de `LIKE '%texto%'`

Un comodín a la izquierda impide que MySQL aproveche un índice B-Tree. Para el
volumen de esta prueba es irrelevante, pero conviene decirlo explícitamente: el
siguiente paso natural ante un volumen alto sería un índice `FULLTEXT`
(`MATCH … AGAINST`) o un motor de búsqueda externo. Se mantiene `LIKE` porque
conserva exactamente la semántica que pide el enunciado, *«que contenga el
texto»*.

---

## Estructura del proyecto

```
src/main/java/com/mobilia/contracts/
├── ContractsBackendApplication.java
├── config/
│   ├── CorsConfig.java              # Orígenes autorizados del front-end
│   ├── MobiliaProperties.java       # Configuración propia, tipada y validada
│   └── OpenApiConfig.java           # Portada de Swagger UI
├── domain/
│   ├── BaseEntity.java              # Id, auditoría, equals/hashCode
│   ├── Person.java
│   ├── Property.java
│   ├── Contract.java
│   ├── ContractParty.java
│   ├── PartyRole.java
│   ├── ContractStatus.java
│   └── PropertyType.java
├── repository/
│   └── ContractRepository.java      # Las dos consultas de la búsqueda
├── service/
│   └── ContractSearchService.java   # Validación, orquestación y patrón LIKE
├── web/
│   ├── ContractController.java
│   ├── dto/
│   │   ├── ContractSearchResponse.java
│   │   ├── PartyResponse.java
│   │   └── PagedResponse.java
│   └── mapper/
│       └── ContractMapper.java      # Agrupa las partes por rol
└── exception/
    ├── GlobalExceptionHandler.java  # Traduce excepciones a ApiError
    ├── InvalidSearchQueryException.java
    └── ApiError.java

src/main/resources/
├── application.yml
└── db/migration/
    ├── V1__create_schema.sql
    └── V2__insert_seed_data.sql
```

Arquitectura **en capas**: `web` → `service` → `repository` → `domain`. Las
dependencias apuntan siempre hacia dentro; el controlador no conoce el
repositorio y el dominio no conoce a nadie.
