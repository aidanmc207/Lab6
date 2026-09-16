# ExpresoFast — Laboratorio 6 (Parte II)

**Plataforma Full-Stack de Logística: Seguridad JWT, Control de Acceso RBAC, DTOs y Bitácora de Auditoría**

| | |
|---|---|
| **Curso** | IF0009 — Desarrollo de Software IV |
| **Carrera** | Informática Empresarial · Sede del Atlántico, Recinto de Paraíso |
| **Profesor** | Mag. Jonathan Granados C. |
| **Semestre** | II-2026 |
| **Laboratorio** | 6 — ExpresoFast Parte II |
| **Estudiante** | *(complete su nombre completo)* |
| **Carné** | C4H845 |
| **Base de datos** | `ExpresoFastC4H845_II2026` |

---

## 1. Requisitos de entorno

| Herramienta | Versión utilizada |
|---|---|
| Java (JDK) | 21 (Temurin 21.0.6 LTS) |
| Maven | 3.9.16 |
| Spring Boot | 3.4.1 (Web, Data JPA, Validation, **Security**) |
| Librería JWT | `io.jsonwebtoken` **jjwt 0.12.6** (`jjwt-api`, `jjwt-impl`, `jjwt-jackson`) |
| Base de datos | Microsoft SQL Server Developer Edition + SSMS 19 |
| Driver JDBC | `com.microsoft.sqlserver:mssql-jdbc` (gestionado por Spring Boot) |
| Navegador | Google Chrome / Microsoft Edge (Chromium) con DevTools |
| Editor | Visual Studio Code (extensión *Live Server* para el frontend) |

---

## 2. Estructura del repositorio

```
expresofast-lab6-c4h845/
├── backend/
│   ├── src/                                Código fuente Spring Boot
│   ├── pom.xml                             Dependencias (incluye Security y jjwt)
│   └── application.properties.template     Plantilla de configuración sin credenciales
├── database/
│   ├── 01_schema_lab5.sql                  DDL base del Lab 5 (4 tablas + datos)
│   ├── 02_schema_lab6_extension.sql        Usuario, Rol, UsuarioRol, BitacoraEnvio
│   └── 03_data_seeds.sql                   Roles, usuarios BCrypt y bitácora inicial
├── frontend/
│   ├── index.html                          Tablero protegido
│   ├── login.html                          Módulo de autenticación
│   ├── styles.css                          CSS3: variables, Flexbox, Grid, modal
│   └── app.js                              fetchWithAuth, RBAC en cliente, bitácora
├── docs/
│   └── ExpresoFast_Postman_Collection.json Colección de pruebas de la API
└── README.md
```

### Paquetes del backend (`cr.ac.ucr.paraiso.ie.c4h845.expresofast`)

| Paquete | Contenido |
|---|---|
| `domain` | `EmpresaLogistica`, `Vehiculo`, `Conductor`, `Envio`, **`Usuario`**, **`Rol`**, **`BitacoraEnvio`**, `AuditableEntity` |
| `dto` | `AuthRequestDTO`, `AuthResponseDTO`, `EnvioRequestDTO`, `EnvioResponseDTO`, `CambioEstadoDTO`, `BitacoraResponseDTO`, `ErrorResponseDTO`, `VehiculoDTO`, `VehiculoRequestDTO`, `ConductorDTO`, `EmpresaDTO`, `ResumenEnviosDTO` |
| `data` | Repositorios `JpaRepository` (JOIN FETCH, `@Modifying`), `UsuarioRepository`, `RolRepository`, `BitacoraEnvioRepository` |
| `business` | `EnvioService`, `AuthService`, `VehiculoService`, `CatalogoService` |
| `security` | `JwtTokenProvider`, `JwtAuthenticationFilter`, `UsuarioDetailsService`, `JwtAuthenticationEntryPoint`, `JwtAccessDeniedHandler` |
| `config` | `SecurityConfig` (`@EnableWebSecurity`, `@EnableMethodSecurity`), `JpaAuditingConfig` |
| `controller` | `AuthController`, `EnvioController`, `VehiculoController`, `CatalogoController` |
| `exception` | `GlobalExceptionHandler` (`@RestControllerAdvice`), `ResourceNotFoundException`, `ReglaNegocioException`, `InvalidStateTransitionException` |

---

## 3. Configuración de la base de datos

Abra SSMS y ejecute los tres scripts **en orden** (`F5` en cada uno):

| Orden | Script | Qué hace |
|---|---|---|
| 1 | `database/01_schema_lab5.sql` | Crea la base `ExpresoFastC4H845_II2026`, las 4 tablas del Lab 5 con sus PK/FK/UNIQUE/CHECK, los índices y 5 envíos de prueba. |
| 2 | `database/02_schema_lab6_extension.sql` | Crea `Usuario`, `Rol`, `UsuarioRol` (PK compuesta) y `BitacoraEnvio` con sus llaves foráneas e índices. |
| 3 | `database/03_data_seeds.sql` | Inserta los 3 roles, los usuarios de prueba **con la contraseña ya procesada con BCrypt**, la relación `UsuarioRol` y una bitácora inicial para los envíos del Lab 5. |

También puede ejecutarlos desde consola:

```bash
sqlcmd -S localhost -E -C -i database\01_schema_lab5.sql
sqlcmd -S localhost -E -C -i database\02_schema_lab6_extension.sql
sqlcmd -S localhost -E -C -i database\03_data_seeds.sql
```

> **Sobre las contraseñas.** La columna `password_hash` **nunca** guarda texto plano: almacena el
> hash BCrypt (costo 10, el mismo que declara `SecurityConfig.passwordEncoder()`). Para agregar un
> usuario nuevo genere el hash con `new BCryptPasswordEncoder(10).encode("suClave")` y péguelo en el
> `INSERT`; el `AuthenticationManager` compara la clave recibida contra ese hash.

### Configurar la conexión

`backend/src/main/resources/application.properties` **no se versiona** (está en `.gitignore`)
porque contiene las credenciales de la máquina local. Tras clonar el repositorio, copie la
plantilla y ajuste sus credenciales:

```bash
cp backend/application.properties.template backend/src/main/resources/application.properties
```

Luego edite `spring.datasource.username` / `spring.datasource.password`. Con instancia nombrada:

```properties
spring.datasource.url=jdbc:sqlserver://localhost\\SQLEXPRESS;databaseName=ExpresoFastC4H845_II2026;encrypt=true;trustServerCertificate=true
```

La clave de firma del JWT se lee de una variable de entorno y solo cae al valor por defecto si no está definida:

```properties
expresofast.jwt.secret=${EXPRESOFAST_JWT_SECRET:...}
expresofast.jwt.expiration-ms=7200000
```

```powershell
# PowerShell, antes de levantar el backend
$env:EXPRESOFAST_JWT_SECRET = "una-clave-propia-de-al-menos-32-caracteres"
```

> HS256 exige una clave de **mínimo 32 caracteres (256 bits)**; con una más corta la aplicación no arranca.

---

## 4. Usuarios de prueba

| Usuario | Contraseña | Rol | Puede hacer |
|---|---|---|---|
| `admin` | `admin123` | `ROLE_ADMIN` | Todo: tablero, registrar envíos, cambiar estados, ver bitácoras y gestionar la flota. |
| `operador1` | `oper123` | `ROLE_OPERADOR` | Tablero, registrar envíos y consultar bitácoras. No cambia estados ni gestiona la flota. |
| `conductor1` | `cond123` | `ROLE_CONDUCTOR` | Tablero y cambio de estado de envíos. No ve el formulario de registro, la gestión de flota ni las bitácoras. |

---

## 5. Instrucciones de ejecución

### Backend

```bash
cd backend
mvn spring-boot:run
```

La API queda en `http://localhost:8080`. Perfil alterno con autenticación de Windows:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=windows
```

### Frontend

Abra la carpeta `frontend/` en VS Code y use **Live Server** sobre `login.html`
(`http://127.0.0.1:5500/login.html`). Inicie sesión con cualquiera de los usuarios de la
tabla anterior; el token se guarda en `localStorage` y el tablero (`index.html`) queda protegido.

> Abrir los `.html` con doble clic (`file://`) hace que el navegador envíe `Origin: null`
> y bloquee las llamadas; use siempre un servidor estático.

---

## 6. Matriz de permisos (RBAC)

| Endpoint | Método | Roles permitidos | Descripción |
|---|---|---|---|
| `/api/auth/login` | `POST` | **Público** | Autenticación y generación del token JWT. |
| `/api/auth/perfil` | `GET` | Autenticado | Datos del portador del token. |
| `/api/envios/optimizados` | `GET` | `ADMIN`, `OPERADOR`, `CONDUCTOR` | Consulta general del tablero (JOIN FETCH, sin N+1). |
| `/api/envios/resumen` | `GET` | `ADMIN`, `OPERADOR`, `CONDUCTOR` | Contadores por estado (KPIs). |
| `/api/envios` | `POST` | `ADMIN`, `OPERADOR` | Registro de un nuevo envío express. |
| `/api/envios/{id}/estado` | `PATCH` | `ADMIN`, `CONDUCTOR` | Cambio de estado (genera bitácora). |
| `/api/envios/{id}/bitacora` | `GET` | `ADMIN`, `OPERADOR` | Consulta del historial de auditoría. |
| `/api/envios/vehiculo/{id}/estado` | `PATCH` | `ADMIN` | Actualización masiva (`@Modifying`). |
| `/api/vehiculos/**` | `ALL` | `ADMIN` | Gestión completa de la flota. |
| `/api/catalogos/**` | `GET` | `ADMIN`, `OPERADOR`, `CONDUCTOR` | Catálogos de apoyo del formulario. |

La regla se declara **dos veces a propósito**: en el `SecurityFilterChain` (por ruta y método) y con
`@PreAuthorize` en cada controlador, para que siga vigente aunque el endpoint cambie de ruta.

---

## 7. Cómo funciona la seguridad

1. `POST /api/auth/login` → `AuthService` delega en el `AuthenticationManager`, que compara la
   contraseña contra el hash BCrypt mediante `UsuarioDetailsService`.
2. `JwtTokenProvider` firma un token **HS256** cuyo `subject` es el `username` y cuyo claim `roles`
   lleva las authorities; la vigencia es de 2 horas.
3. Cada petición posterior pasa por `JwtAuthenticationFilter` (`OncePerRequestFilter`): extrae el
   token de `Authorization: Bearer <token>`, valida firma y vigencia, y carga la autenticación en el
   `SecurityContextHolder`.
4. El `SecurityFilterChain` es **`STATELESS`** (sin `JSESSIONID`) y evalúa la matriz RBAC.
5. Si falta el token → `JwtAuthenticationEntryPoint` responde **401**; si el rol es insuficiente →
   `JwtAccessDeniedHandler` responde **403**. Ambos devuelven el mismo JSON que el
   `GlobalExceptionHandler`.

### Formato único de error

```json
{
  "timestamp": "2026-09-15T21:14:03.118",
  "status": 400,
  "error": "Bad Request",
  "mensaje": "Los datos enviados no superaron la validacion",
  "path": "/api/envios",
  "errores": {
    "codigoRastreo": "Formato invalido. Ejemplo: EXP-1234",
    "pesoKg": "El peso debe ser mayor a cero"
  }
}
```

| Excepción | Código |
|---|---|
| `MethodArgumentNotValidException` | `400` + detalle campo por campo |
| `InvalidStateTransitionException`, `ReglaNegocioException` | `400` |
| `ResourceNotFoundException` | `404` |
| `BadCredentialsException` / `DisabledException` | `401` |
| `AccessDeniedException` | `403` |
| `Exception` (genérica) | `500` — se registra en el log del servidor, **nunca** se devuelve el stacktrace |

---

## 8. Bitácora de auditoría

Cada cambio de estado escribe automáticamente una fila en `BitacoraEnvio` dentro de la misma
transacción: estado anterior, estado nuevo, fecha/hora, **usuario tomado del `SecurityContextHolder`**
y las observaciones que envía el cliente.

```http
PATCH /api/envios/1/estado
Authorization: Bearer <token>
Content-Type: application/json

{ "nuevoEstado": "EN_TRANSITO", "observaciones": "Paquete cargado en la unidad" }
```

En el tablero, el botón **“Ver bitácora”** (visible solo para `ADMIN` y `OPERADOR`) abre un modal
de posición fija con la línea de tiempo del envío y **dos campos `<input type="date">` que filtran
el historial por rango de fechas**.

---

## 9. Reglas de negocio (reto autónomo)

Secuencia de estados válida, aplicada en `EnvioService.validarTransicion()`:

```
PENDIENTE ──► EN_TRANSITO ──► ENTREGADO
    │                │
    └────────────────┴────► CANCELADO
```

* `ENTREGADO` y `CANCELADO` son **estados finales**: no pueden volver a `PENDIENTE` ni a `EN_TRANSITO`.
* Tampoco se admite repetir el estado actual.
* Cualquier violación lanza `InvalidStateTransitionException`, que el `GlobalExceptionHandler`
  traduce a **400 Bad Request** con el mensaje `Transición de estado no permitida para el envío EXP-9003`.

Se conservan además las reglas del Lab 5: código de rastreo único, vehículo fuera de
`MANTENIMIENTO` y validación de capacidad (peso individual y carga acumulada del vehículo).

---

## 10. Pruebas con Postman

Importe `docs/ExpresoFast_Postman_Collection.json`. La colección incluye:

* Login para los tres roles; los scripts de test guardan `{{token_admin}}`, `{{token_operador}}` y
  `{{token_conductor}}` automáticamente.
* Casos positivos de cada endpoint de la matriz.
* Casos negativos: **401** sin token, **401** con credenciales inválidas, **403** por rol
  insuficiente, **400** por validación de DTO y **400** por transición de estado inválida.

Ejecute primero *Autenticacion → 01 Login ADMIN* y luego el resto con el *Collection Runner*.

---

## 11. Diagnóstico: bloqueo de CORS en el pre-vuelo `OPTIONS`

**Síntoma en la consola del navegador**

```
Access to fetch at 'http://localhost:8080/api/envios' from origin 'http://127.0.0.1:5500'
has been blocked by CORS policy: Response to preflight request doesn't pass access control
check: It does not have HTTP ok status.
```

**Causa.** Antes de un `POST`/`PATCH`/`DELETE` el navegador envía una petición `OPTIONS` **sin**
el encabezado `Authorization`. Si Spring Security la evalúa como una ruta protegida, responde
401/403 y el navegador cancela la llamada real.

**Solución aplicada** (`SecurityConfig` + `JwtAuthenticationFilter`):

1. `.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()` **como primera regla**, antes de las rutas protegidas.
2. `JwtAuthenticationFilter.shouldNotFilter()` devuelve `true` para `OPTIONS`, de modo que el
   pre-vuelo ni siquiera intenta validar un token inexistente.
3. Un único `CorsConfigurationSource` declarado como bean (`cors(Customizer.withDefaults())`), sin
   `@CrossOrigin` duplicados que producirían encabezados `Access-Control-Allow-Origin` repetidos.

**Verificación.** Reinicie el backend, limpie la caché y confirme en *DevTools → Network* que la
solicitud `OPTIONS` responde **200 OK** con `Access-Control-Allow-Origin`.

---

## 12. Cambios respecto al Laboratorio 5

* Repositorio reorganizado en `backend/`, `database/`, `frontend/` y `docs/`.
* `EnvioDTO` → **`EnvioResponseDTO`** (incluye `placaVehiculo` y `nombreConductor`).
* `EnvioRequestDTO` ahora recibe `vehiculoId` / `conductorId` planos y valida con
  `@NotBlank`, `@Pattern(^EXP-\d{4}$)` y `@Positive`.
* `CambioEstadoDTO` pasó de `estadoEnvio` a **`nuevoEstado` + `observaciones`**.
* `ManejadorGlobalExcepciones` → **`GlobalExceptionHandler`** con respuesta `ErrorResponseDTO`.
* `RecursoNoEncontradoException` → **`ResourceNotFoundException`**.
* `WebCorsConfig` se elimina: el CORS lo centraliza `SecurityConfig`.
* Ningún endpoint devuelve entidades JPA: `CatalogoController` ahora usa `EmpresaDTO`.

---

## 13. Entrega

* Repositorio GitHub: `expresofast-lab6-c4h845`
* URL para Mediación Virtual: `https://github.com/<usuario>/expresofast-lab6-c4h845`
* Si el repositorio es privado, agregar como colaborador al usuario **`jgranadosc`**.
