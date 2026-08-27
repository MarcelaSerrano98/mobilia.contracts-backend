---
name: deploy
description: Automatiza la verificación, compilación y despliegue del backend de Spring Boot a producción.
disable-model-invocation: true
allowed-tools: Bash, Read
---

# Flujo de Despliegue de Producción (Backend)

## Objetivo de la Skill

Garantizar que el backend de Spring Boot compile sin errores, pase las pruebas
y esté listo para subirse al servidor de producción (Render, Railway o Docker).

> **Esta skill NO se activa sola.** `disable-model-invocation: true` hace que
> solo se ejecute cuando la persona usuaria escribe `/deploy`. Un despliegue no
> debe dispararse porque el agente interprete que toca.

---

## Datos de este proyecto

Comprobados contra el repositorio; no hace falta volver a averiguarlos:

| | |
|---|---|
| Gestor de construcción | **Maven** (`pom.xml`) |
| Comando | **`./mvnw`** — el wrapper. `mvn` **no** está instalado globalmente en la máquina de desarrollo, así que `mvn clean test` fallaría con `command not found` |
| Java | 21 (LTS) |
| Artefacto | `target/contracts-backend-0.0.1-SNAPSHOT.jar` |
| `Dockerfile` | **No existe.** Sólo hay `compose.yaml`, y ése levanta MySQL en local, no empaqueta la aplicación |
| Variables que lee `application.yml` | `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `SERVER_PORT`, `CORS_ALLOWED_ORIGINS`, `LOG_LEVEL` |

---

## Pasos del Despliegue

### 1. Verificación de seguridad

Ejecuta los tests para asegurar que ningún cambio reciente rompa el backend:

```bash
./mvnw clean test      # unitarios y de capa web; no necesitan Docker
./mvnw verify          # además los de integración contra MySQL real
```

`verify` es el que da la garantía completa. Si no hay Docker, los tests de
integración se **omiten** en lugar de fallar, así que un `verify` verde sin
Docker cubre menos de lo que parece: dilo explícitamente en el reporte.

### 2. Construcción del artefacto

```bash
./mvnw clean package -DskipTests
```

Comprueba después que el `.jar` existe y anota su tamaño:

```bash
ls -lh target/*.jar
```

> Se salta los tests a propósito porque ya se ejecutaron en el paso 1.
> **Si el paso 1 falló, no continúes**: un artefacto construido sobre tests
> rojos es exactamente lo que este flujo debe impedir.

### 3. Comprobación de Docker

No hay `Dockerfile` en el repositorio. Verifícalo antes de dar nada por hecho:

```bash
ls Dockerfile 2>/dev/null || echo "sin Dockerfile"
```

Si sigue sin existir, informa de ello y ofrece crear uno multi-etapa: la primera
etapa construye con el JDK, la segunda copia sólo el `.jar` sobre una imagen JRE
mínima. No lo crees sin que te lo pidan.

### 4. Variables de entorno en la nube

`application.yml` usa `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER` y
`DB_PASSWORD`, no `SPRING_DATASOURCE_*`. Aun así **las dos formas funcionan**,
porque en Spring Boot las variables de entorno tienen mayor precedencia que
`application.yml`. Comprobado en este proyecto: al arrancar con
`SPRING_DATASOURCE_URL` apuntando a una base inexistente, el error nombraba esa
base y no la del fichero.

Orden de precedencia, de mayor a menor: argumentos de línea de comandos →
variables de entorno → `application-{perfil}.yml` → `application.yml` → valores
por defecto del código.

Las dos formas de configurarlo en el panel del proveedor:

```bash
# Opción A — las variables propias del proyecto
DB_HOST=mi-host.proveedor.com
DB_PORT=3306
DB_NAME=mobilia_contracts
DB_USER=usuario_produccion
DB_PASSWORD=***

# Opción B — las estándar de Spring Boot
SPRING_DATASOURCE_URL=jdbc:mysql://mi-host:3306/mobilia_contracts?useSSL=true
SPRING_DATASOURCE_USERNAME=usuario_produccion
SPRING_DATASOURCE_PASSWORD=***
```

En ambos casos hay que ajustar además:

```bash
SERVER_PORT=8080                              # Render y Railway inyectan PORT
CORS_ALLOWED_ORIGINS=https://mi-front.com     # el origen real del front-end
```

Recuerda tres cosas al reportar:

- **La contraseña nunca se escribe en el repositorio.** Va en el panel de
  variables del proveedor. Si aparece en algún fichero versionado, detén el
  despliegue y avisa.
- `useSSL=true` en producción; el `useSSL=false` de `application.yml` es un
  valor por defecto para desarrollo local.
- Flyway aplicará las migraciones contra la base de producción en el primer
  arranque. Confirma que es lo que se espera antes de desplegar.

### 5. Reporte final

Resume en una tabla: resultado de los tests (cuántos, cuántos omitidos y por
qué), si el `.jar` se generó y con qué tamaño, si hay `Dockerfile`, y qué
variables faltan por configurar.

Después indica los comandos para publicar los cambios:

```bash
git status                    # revisar qué se va a subir
git add -A
git commit -m "..."           # conventional commits
git push origin main
```

> El repositorio tiene un guardarraíl que bloquea comandos git destructivos
> (`reset --hard`, `clean -f`, `push --force`). `git push` normal **sí** está
> permitido. Si un comando aparece bloqueado, no es un fallo: es el hook
> haciendo su trabajo.

Si el proveedor tiene despliegue automático conectado al repositorio, el `push`
lo dispara. Dilo explícitamente para que quede claro que no hay un paso manual
adicional.
