# Bitácora de desarrollo

Registro cronológico de cómo se construyó esta prueba técnica: qué se hizo en
cada fase, por qué se tomó cada decisión, qué problemas aparecieron y cómo se
resolvieron.

El enunciado pide expresamente *«ayudarse con la IA para el desarrollo de la
prueba»*. Este documento deja constancia de ese proceso: no sólo del resultado,
sino del camino — incluidos los errores que hubo que diagnosticar.

- **Repositorio back-end** · https://github.com/MarcelaSerrano98/mobilia.contracts-backend
- **Repositorio front-end** · https://github.com/MarcelaSerrano98/mobilia.contracts-frontend

---

## Índice

1. [Punto de partida](#1-punto-de-partida)
2. [Análisis del enunciado](#2-análisis-del-enunciado)
3. [Corrección de la base del proyecto](#3-corrección-de-la-base-del-proyecto)
4. [Modelo de datos](#4-modelo-de-datos)
5. [Entidades JPA](#5-entidades-jpa)
6. [La búsqueda](#6-la-búsqueda)
7. [Capa web](#7-capa-web)
8. [Pruebas](#8-pruebas)
9. [Documentación](#9-documentación)
10. [Front-end](#10-front-end)
11. [Publicación y correcciones](#11-publicación-y-correcciones)
12. [Herramientas de desarrollo](#12-herramientas-de-desarrollo)
13. [Auditoría final](#13-auditoría-final)
14. [Problemas encontrados](#14-problemas-encontrados)
15. [Historial de commits](#15-historial-de-commits)

---

## 1. Punto de partida

El repositorio existía con la estructura generada por Spring Initializr
(`77b32c4`): `pom.xml`, wrapper de Maven, `.gitignore` y la clase principal.

Lo que ya estaba bien y no se tocó:

- `groupId com.mobilia` / `artifactId contracts-backend`.
- Flyway incluido entre las dependencias — decisión afortunada, porque el
  enunciado pregunta literalmente «qué scripts de base de datos hay que
  ejecutar».
- `.gitignore` cubriendo IntelliJ IDEA, Eclipse/STS, NetBeans y VS Code, que es
  un requisito explícito del enunciado.

---

## 2. Análisis del enunciado

Antes de escribir código se extrajo el texto del PDF y se convirtió en una lista
de requisitos verificables, agrupados en cuatro bloques: componentes del modelo,
desarrollo deseado, condiciones y entregables.

Del análisis salieron dos conclusiones que dirigieron todo lo demás:

**La parte difícil no es la búsqueda, son las reglas de cardinalidad.** Un
inmueble admite como máximo un contrato activo; un contrato tiene exactamente un
arrendatario, uno o más propietarios y cero o más deudores solidarios. Ahí es
donde se juega el diseño.

**«Historial» implica que los contratos inactivos también se consultan.** Por eso
la respuesta de la API incluye el estado del contrato, aunque el enunciado no lo
liste entre las columnas de la tabla: sin él, el historial no se distingue de lo
vigente.

---

## 3. Corrección de la base del proyecto

**Commit `7985477`.**

| Cambio | Motivo |
|---|---|
| Java 25 → **21** | Java 21 es LTS. Con 25, quien tuviera JDK 17 o 21 no podría ni compilar el proyecto, y «no pude levantarlo» es la peor nota posible |
| Spring Boot 4.1.1 → **3.5.9** | Toda la documentación y las respuestas de la comunidad están sobre 3.x. Poder explicar cada decisión pesa más que usar lo último |
| `com.mobilia.contracts_backend` → **`com.mobilia.contracts`** | Los nombres de paquete en Java no admiten guion bajo, y el enunciado exige seguir las convenciones de nombramiento |
| `application.properties` → **`application.yml`** | Configuración externalizada por variables de entorno con valores por defecto |
| Metadatos vacíos del `pom.xml` | `<license/>` y `<developer/>` vacíos son restos del Initializr |
| Nuevo `compose.yaml` | Levanta MySQL sin instalar nada |

---

## 4. Modelo de datos

**Commit `8944a34`.** Es el núcleo de la prueba.

### La tabla `contract_party`

La tentación es poner `arrendatario_id`, `propietario_id` y `deudor_id` como
columnas de `contract`. Eso rompe con «1 o más propietarios»: haría falta
`propietario_1`, `propietario_2`… que es un **grupo repetitivo** y viola la
Primera Forma Normal.

`contract_party` resuelve una relación muchos-a-muchos entre contrato y persona
**cualificada por un atributo**, el rol. Permite cardinalidades ilimitadas y que
la misma persona sea propietaria en un contrato e inquilina en otro sin duplicar
su registro.

### Las columnas generadas

MySQL no soporta índices únicos parciales. La regla «máximo un contrato activo
por inmueble» se expresa con una columna calculada por el motor:

```sql
active_property_id BIGINT GENERATED ALWAYS AS
    (CASE WHEN status = 'ACTIVO' THEN property_id END) VIRTUAL,
CONSTRAINT uk_contract_one_active_per_property UNIQUE (active_property_id)
```

Se apoya en que **MySQL ignora los `NULL` en los índices únicos**: los contratos
inactivos generan `NULL` y nunca colisionan; dos activos del mismo inmueble
producen el mismo valor y el motor rechaza la operación.

El mismo patrón garantiza «máximo un arrendatario por contrato».

### Otras decisiones del esquema

- **`VARCHAR` + `CHECK` en lugar del `ENUM` de MySQL**: portable, su `ALTER` no
  es costoso, y encaja con `@Enumerated(EnumType.STRING)`.
- **Colación `utf8mb4_0900_ai_ci`**: la búsqueda ignora mayúsculas *y tildes*.
  Buscar `nunez` encuentra `Núñez` sin normalizar nada en el código.
- **Datos de ejemplo** que ejercitan todas las cardinalidades: un inmueble con un
  contrato activo y dos inactivos, un contrato con dos propietarios, otro con dos
  deudores solidarios, otro sin ninguno, y personas que cambian de rol entre
  contratos.

### Verificación

Antes de seguir se aplicaron las migraciones a una base de pruebas y se
comprobó, con ocho `INSERT` deliberadamente inválidos, que el esquema **rechaza
lo que debe rechazar**: segundo contrato activo, segundo arrendatario, persona
duplicada con el mismo rol, tipo de inmueble inexistente y documento repetido.

---

## 5. Entidades JPA

**Parte del commit `6db9ccf`.**

- `BaseEntity` (`@MappedSuperclass`) concentra el identificador, la auditoría y
  la igualdad.
- **No se usa `@Data` de Lombok en las entidades**: su `equals` compara todos los
  campos, lo que fuerza la carga de las asociaciones `LAZY`. La igualdad se
  implementa por identificador, comparando con `Hibernate.getClass()` porque una
  entidad perezosa es en realidad un *proxy*.
- Todas las asociaciones `@ManyToOne` declaran `FetchType.LAZY` de forma
  explícita: el valor por defecto es `EAGER`.
- `@Enumerated(EnumType.STRING)` siempre. Con `ORDINAL` — que es el valor por
  defecto si se omite — se persiste la posición de la constante, de modo que
  reordenar el `enum` corrompe en silencio los datos ya guardados.
- Las columnas generadas del esquema **no se mapean**: las calcula MySQL, y la
  validación de Hibernate sólo comprueba que las columnas mapeadas existan.

---

## 6. La búsqueda

**Parte del commit `6db9ccf`.** Es la decisión menos obvia del proyecto.

### Por qué dos consultas y no una

Si se busca «Gómez» y esa persona es **propietaria**, el contrato debe aparecer
— pero la tabla tiene que mostrar **también** al arrendatario y a los deudores
solidarios, que no contienen el texto buscado.

Filtrar y proyectar en la misma consulta devolvería únicamente las partes
coincidentes. Es un error silencioso: la respuesta parece correcta, sólo que
incompleta.

Además, combinar `JOIN FETCH` de una colección con paginación obliga a Hibernate
a traer todas las filas y paginar **en memoria** (aviso `HHH90003004`).

La solución son dos pasos:

1. **Qué contratos coinciden** — se filtra y pagina sobre identificadores.
2. **Cargarlos completos** — una única consulta con `JOIN FETCH` de inmueble,
   partes y personas, sin problema N+1.

### `EXISTS` en lugar de `JOIN`

Un `JOIN` con las partes multiplica filas: un contrato con tres partes
coincidentes aparecería tres veces, lo que obliga a un `DISTINCT`. Y con
`DISTINCT`, MySQL prohíbe ordenar por una columna ausente de la proyección
(error 3065). Con `EXISTS` cada contrato aparece una sola vez y el conteo de la
paginación es correcto.

### Sin `LOWER()`

La insensibilidad a mayúsculas y tildes la aporta la colación. `LOWER()` daría
una falsa sensación de portabilidad, porque no resuelve las tildes en ningún
motor.

### Escapado de comodines

El texto se escapa antes de construir el patrón `LIKE`: sin ello, teclear `%`
devolvería todos los contratos. **No es una defensa contra inyección SQL** —el
valor viaja como parámetro enlazado, así que eso ya está cubierto—, sino un
problema de corrección del resultado.

---

## 7. Capa web

**Parte del commit `6db9ccf`.**

- `GET /api/v1/contracts/search?q=…&page=…&size=…`. La ruta lleva versión para
  poder publicar otro formato de respuesta sin romper a los consumidores.
- **DTOs como `record`**, nunca las entidades: no se filtra el modelo interno,
  Jackson no recorre asociaciones perezosas fuera de la transacción, y el
  contrato de la API queda desacoplado del esquema.
- **`PagedResponse` propio** en lugar de serializar `PageImpl`: Spring Data lo
  desaconseja desde Boot 3.3 porque su forma JSON no tiene garantía de
  compatibilidad entre versiones.
- `@RestControllerAdvice` con un formato de error uniforme, para que el
  front-end trate los fallos en un solo lugar.
- CORS y límites de búsqueda mediante `@ConfigurationProperties` tipado y
  validado al arrancar.
- `open-in-view: false`: el valor por defecto de Spring Boot mantiene la sesión
  de Hibernate abierta durante el renderizado y **oculta** los problemas de
  *fetching* hasta producción.
- Swagger UI en `/swagger-ui.html`.

---

## 8. Pruebas

**Commits `24ac80b` y `e986249`.** 48 tests, ninguno fallando.

| Tipo | Clase | Qué cubre |
|---|---|---|
| Unitario | `ContractSearchServiceTest` | Validación, escapado de comodines, límites de paginación, agrupación por rol |
| Capa web | `ContractControllerTest` | Códigos HTTP, forma del JSON, respuestas de error |
| Integración | `ContractRepositoryIT` | Los seis campos de búsqueda, tildes, paginación y **ausencia de N+1** |
| Integración | `SchemaConstraintsIT` | Que la **base de datos** rechaza los estados inválidos |
| Integración | `ContractInvariantsIT` | Las cardinalidades **mínimas** del enunciado |
| Integración | `ContractsBackendApplicationTests` | Que Flyway migra y Hibernate valida el esquema |

**Testcontainers en lugar de H2.** El esquema depende de características que sólo
existen en MySQL: columnas generadas, índices únicos que ignoran `NULL` y la
colación `ai_ci`. Probar sobre H2 validaría un esquema distinto del que se
despliega.

**Los tests se omiten sin Docker, no fallan**
(`@Testcontainers(disabledWithoutDocker = true)`), de modo que `mvn test` sigue
en verde en cualquier máquina. `maven-failsafe-plugin` separa la pirámide:
Surefire recoge `*Test`, Failsafe recoge `*IT`.

**La ausencia de N+1 se comprueba de verdad**: se recorre el grafo completo y se
verifica con las estadísticas de Hibernate que el contador de sentencias sigue
en uno.

---

## 9. Documentación

**Commits `fd0bf2f` y `d16e2ff`.**

`README.md` con guía de ejecución por dos vías (Docker o MySQL local), tabla de
variables de entorno, el modelo de datos explicado, el contrato de la API,
resolución de problemas y las decisiones técnicas con su justificación.

La pregunta del enunciado —«qué scripts de base de datos hay que ejecutar»—
tiene respuesta explícita: **ninguno**, los aplica Flyway al arrancar.

---

## 10. Front-end

Repositorio aparte, según admite el enunciado.

- **React 19 + Vite + TypeScript.** El contrato de la API queda tipado: si el
  back-end cambia un campo, avisa el compilador y no una pantalla en blanco.
- `api/` concentra la comunicación HTTP; ningún componente llama a `fetch`
  directamente.
- `useContractSearch` gestiona los cuatro estados de la búsqueda —`idle`,
  `loading`, `success`, `error`— y **cancela la petición anterior con
  `AbortController`**: sin eso, una respuesta lenta podría sobrescribir en
  pantalla un resultado más reciente.
- Tabla HTML semántica con `<th scope="col">` y `<caption>`, y mensajes de estado
  anunciados con `aria-live`.
- El `key` de React es el documento de identidad, no el índice del array.
- Sin Axios ni Redux: una sola petición `GET` y un estado que cabe en un hook no
  los justifican.

---

## 11. Publicación y correcciones

Ambos repositorios publicados en GitHub, con enlaces cruzados en los dos
`README`.

**Commit `127ea56`** corrige un fallo que habría afectado a quien evaluara la
prueba: `docker compose up -d` fallaba con `bind: address already in use` en
cualquier máquina con un MySQL ya instalado en el puerto 3306.

La primera solución tenía **dos** variables —una para Docker y otra para Spring—
y nada garantizaba que coincidieran. En una máquina con MySQL instalado el fallo
ni siquiera saltaba: la aplicación acababa hablando con la base local en lugar
de con el contenedor, sin avisar. La solución definitiva reutiliza `DB_PORT`
para ambos lados, de modo que es imposible desincronizarlos.

---

## 12. Herramientas de desarrollo

**Commits `c30b174` y `56ca607`.** El enunciado pide ayudarse con la IA, así que
la configuración de herramientas se versiona junto al código.

| Herramienta | Qué hace |
|---|---|
| `.claude/skills/verify-db-rules/` | Audita el esquema y las entidades frente a las reglas del enunciado |
| `.claude/skills/deploy/` | Flujo de despliegue. **Sólo se activa manualmente** con `/deploy` |
| `.claude/hooks/block-dangerous-git.sh` | Bloquea comandos git destructivos antes de ejecutarlos |

El guardarraíl de git se adaptó de una skill de terceros con tres cambios: no
bloquea `git push` a secas (no destruye nada), distingue *invocar* git de
*mencionarlo* en un texto, y **falla cerrado** si `jq` no está disponible —el
original permitía todo en ese caso, haciendo desaparecer el guardarraíl sin
avisar.

---

## 13. Auditoría final

Se releyó el PDF y se verificó **requisito por requisito contra el código**, no
contra la memoria. Todo cumple.

Dos hallazgos merecen mención porque se comprobaron antes de reportarlos:

- Un audit marcó seis métodos con nombre incorrecto. Al abrirlos resultaron ser
  declaraciones de `record`, que la expresión regular leía como métodos.
- Otro marcó un `@ManyToOne` sin `LAZY`. Era un comentario Javadoc que
  *mencionaba* la anotación.

El único requisito que se cumplía **sin verificar** era «cada contrato tiene
como mínimo 2 personas». Los datos lo satisfacían, pero nada lo comprobaba. Un
máximo se expresa impidiendo la segunda fila; un mínimo no tiene equivalente
declarativo en ningún motor relacional, porque al insertar el contrato todavía
no existe ninguna parte que apunte a él. Se cerró con `ContractInvariantsIT`.

---

## 14. Problemas encontrados

Los siete que hubo que diagnosticar, en orden de aparición.

### 1 · `ERROR 1215` al crear la clave foránea

La migración fallaba con `Cannot add foreign key constraint`. MySQL prohíbe
`ON DELETE CASCADE` sobre la columna base de una columna generada `STORED`, y
`contract_id` era ambas cosas a la vez. Se diagnosticó probando las dos
variantes por separado en una tabla mínima. Solución: `VIRTUAL`.

### 2 · Lombok dejó de generar código

Los *getters* no existían al compilar. Desde el JDK 23, `javac` ya no ejecuta de
forma implícita los procesadores de anotaciones que encuentra en el classpath.
Solución: declarar Lombok en `annotationProcessorPaths`.

### 3 · Dos beans del mismo tipo en `@WebMvcTest`

`@EnableConfigurationProperties` de la clase principal ya registraba
`MobiliaProperties`; una `@TestConfiguration` creaba un segundo y el contexto no
arrancaba. Solución: eliminar el bean redundante del test.

### 4 · `strict` desactivado en TypeScript

El scaffold de Vite no lo activa. Sin `strictNullChecks`, el tipo
`Party | null` no aporta nada: el compilador acepta el acceso y el fallo aparece
en tiempo de ejecución. Se comprobó quitando el acceso seguro a propósito, que
con `strict` produce `error TS18047`.

### 5 · `| head -1` ocultaba el error de MySQL

Al verificar las restricciones, el `ERROR 1062` desaparecía del *pipe* y sólo
quedaba el aviso de contraseña, de modo que los `INSERT` inválidos parecían
haber tenido éxito. Se confirmó contando filas antes de concluir nada.

### 6 · Falsos positivos por `grep`

Descritos en la auditoría final. El mismo patrón apareció en el guardarraíl de
git, que bloqueaba `git commit -m "explica git push"` porque el mensaje contenía
la cadena.

### 7 · El tercer test de integración rompió a los otros dos

Al añadir `ContractInvariantsIT`, sus cuatro tests fallaban con
`CannotCreateTransaction` tras **125 segundos** de espera.

La causa es un choque entre dos políticas de ciclo de vida: la extensión de
JUnit detiene los contenedores `@Container` al terminar **cada clase**, mientras
Spring cachea el contexto **entre clases**. La tercera clase tenía las mismas
anotaciones que la primera, así que reutilizó su contexto cacheado, que apuntaba
a un contenedor ya detenido. Solución: el patrón *singleton* de Testcontainers.
Como efecto secundario, los tests de integración bajaron de unos 12 s a unos 7 s.

> **El hilo común de los problemas 4, 5, 6 y 7:** una comprobación que falla en
> silencio es peor que no tener ninguna, porque genera confianza injustificada.

---

## 15. Historial de commits

### Back-end

| Commit | Descripción |
|---|---|
| `77b32c4` | `chore:` estructura inicial con Spring Initializr |
| `7985477` | `chore:` alinea la configuración con los requisitos de la prueba |
| `8944a34` | `feat(db):` modela el esquema con Flyway |
| `6db9ccf` | `feat(api):` implementa la búsqueda por texto libre |
| `24ac80b` | `test:` cubre servicio, capa web, repositorio y restricciones |
| `fd0bf2f` | `docs:` guía de ejecución y decisiones técnicas |
| `d16e2ff` | `docs:` enlaza el repositorio del front-end |
| `127ea56` | `fix(docker):` permite publicar MySQL en otro puerto |
| `c30b174` | `chore(claude):` skill de auditoría de reglas de negocio |
| `56ca607` | `chore(claude):` guardarraíl contra comandos git destructivos |
| `85a127e` | `fix(test):` comparte una única instancia de MySQL entre las clases |
| `e986249` | `test:` verifica las cardinalidades mínimas del enunciado |

### Front-end

| Commit | Descripción |
|---|---|
| `c6ff2b5` | `feat:` pantalla de búsqueda con React, Vite y TypeScript |
| `ae1988b` | `fix:` activa las comprobaciones estrictas de TypeScript |
| `d4a8ea1` | `chore:` versiona la skill de diseño del front-end |
| `17e5f5b` | `docs:` explica cómo liberar el puerto 5173 |
| `320bb38` | `feat:` rediseña la consulta como historial de inmuebles |
| `16d9d6b` | `docs:` marca en el código los conceptos clave |
| `69dd8ce` | `refactor:` unifica la longitud mínima y aclara la celda sin deudores |

---

## Estado final

| | |
|---|---|
| Tests | **48**, ninguno fallando (`./mvnw verify`) |
| Requisitos del enunciado | Todos verificados contra el código |
| Convenciones Java y JavaScript | 0 violaciones |
| Repositorios | Publicados y sincronizados |
