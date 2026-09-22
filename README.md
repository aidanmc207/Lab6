# ExpresoFast — Laboratorio 8 (Parte IV)

**Integración Pila Completa: Consola de Operación Logística con backend REST, HTML5 semántico y CSS3 responsivo**

| | |
|---|---|
| **Curso** | IF0009 — Desarrollo de Software IV |
| **Carrera** | Informática Empresarial · Sede del Atlántico, Recinto de Paraíso |
| **Profesor** | Mag. Jonathan Granados C. |
| **Semestre** | II-2026 |
| **Laboratorio** | 8 — ExpresoFast Parte IV |
| **Estudiante** | *(complete su nombre completo)* |
| **Carné** | C4H845 |
| **Base de datos** | `ExpresoFastC4H845_II2026` |

Este repositorio acumula las cuatro entregas: persistencia con Spring Data JPA (Lab 5),
seguridad JWT y RBAC (Lab 6), pruebas con JUnit 5 y Mockito (Lab 7) y la consola web
cliente (Lab 8).

---

## 1. Requisitos de entorno

| Herramienta | Versión utilizada |
|---|---|
| Java (JDK) | 21 (Temurin 21.0.6 LTS) |
| Maven | 3.9.16 |
| Spring Boot | 3.4.1 (Web, Data JPA, Validation, Security) |
| Librería JWT | `io.jsonwebtoken` jjwt 0.12.6 |
| Base de datos | Microsoft SQL Server Developer Edition + SSMS 19 |
| Navegador | Google Chrome / Microsoft Edge con DevTools |
| Editor | Visual Studio Code + extensión **Live Server** |

---

## 2. Estructura del repositorio

```
expresofast-lab6-c4h845/
├── backend/                                Proyecto Spring Boot (API REST)
│   ├── src/main/                           Código de la aplicación
│   ├── src/test/                           Suite JUnit 5 + Mockito (83 pruebas)
│   ├── pom.xml
│   └── application.properties.template
├── database/
│   ├── 01_schema_lab5.sql                  DDL base (4 tablas + datos)
│   ├── 02_schema_lab6_extension.sql        Usuario, Rol, UsuarioRol, BitacoraEnvio
│   └── 03_data_seeds.sql                   Roles, usuarios BCrypt y bitácora inicial
├── frontend/                               Consola web cliente
│   ├── index.html                          Vista 1 · autenticación
│   ├── dashboard.html                      Vista 2 · consola de operaciones
│   ├── styles.css                          Variables CSS, Grid, Flexbox, responsivo
│   └── app.js                              Fetch API, JWT y renderizado por rol
├── docs/
│   └── ExpresoFast_Postman_Collection.json
└── README.md
```

---

## 3. Puesta en marcha

### Paso 1 — Base de datos

Ejecute los tres scripts **en orden** desde SSMS (`F5`) o por consola:

```bash
sqlcmd -S localhost -U sa -P <password> -C -b -i database\01_schema_lab5.sql
sqlcmd -S localhost -U sa -P <password> -C -b -i database\02_schema_lab6_extension.sql
sqlcmd -S localhost -U sa -P <password> -C -b -i database\03_data_seeds.sql
```

Son idempotentes: puede volver a correrlos para dejar la base en el estado de referencia.

### Paso 2 — Configuración

`backend/src/main/resources/application.properties` **no se versiona** (está en `.gitignore`)
porque contiene las credenciales de la máquina local. Tras clonar:

```bash
cp backend/application.properties.template backend/src/main/resources/application.properties
```

Complete `spring.datasource.username` / `spring.datasource.password`. La clave de firma del
token se toma de una variable de entorno:

```powershell
$env:EXPRESOFAST_JWT_SECRET = "una-clave-propia-de-al-menos-32-caracteres"
```

### Paso 3 — Levantar la API REST

```bash
cd backend
mvn spring-boot:run
```

La API queda en `http://localhost:8080`. Para correr la suite de pruebas:

```bash
mvn test
```

### Paso 4 — Desplegar el cliente

El cliente **debe servirse desde un servidor HTTP**, no abrirse con doble clic: con `file://`
el navegador envía `Origin: null` y la política CORS lo rechaza.

1. Abra la carpeta `frontend/` en Visual Studio Code.
2. Clic derecho sobre `index.html` → **Open with Live Server**.
3. El navegador abre `http://127.0.0.1:5500/index.html`.

Cualquier servidor estático en el puerto **5500** sirve, porque son los orígenes que
`WebConfig` tiene autorizados.

---

## 4. Usuarios de prueba

| Usuario | Contraseña | Rol | Qué ve en la consola |
|---|---|---|---|
| `admin` | `admin123` | `ROLE_ADMIN` | Todos los envíos, el panel lateral de bitácora, el alta de vehículos y todas las transiciones de estado. |
| `operador1` | `oper123` | `ROLE_OPERADOR` | Todos los envíos y el botón **Despachar** sobre los envíos `PENDIENTE`. |
| `conductor1` | `cond123` | `ROLE_CONDUCTOR` | Únicamente los envíos asignados a su ficha de conductor y el botón **Marcar entregado**. |

---

## 5. Configuración CORS

`backend/.../config/WebConfig.java` implementa `WebMvcConfigurer` y es la **única** fuente
de la política:

```java
registry.addMapping("/api/**")
        .allowedOrigins("http://localhost:5500", "http://127.0.0.1:5500")
        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(true);
```

Además, `SecurityConfig` permite el pre-vuelo `OPTIONS` como **primera regla** de la cadena
y `JwtAuthenticationFilter` no lo intercepta, porque esa petición viaja sin token. Sin esas
dos piezas el navegador cancela toda llamada `POST`/`PUT` con un error de CORS.

---

## 6. Vistas del cliente

### `index.html` — Autenticación

Formulario `#loginForm` con `<label for>` vinculados a `#username` y `#password`, y una caja
de error con `role="alert"` y `aria-live="polite"` que anuncia los fallos de credenciales a
los lectores de pantalla.

### `dashboard.html` — Consola de operaciones

| Elemento | Contenido |
|---|---|
| `<header>` | Logotipo, nombre del usuario autenticado, sus roles y el botón de cierre de sesión. |
| `<nav>` | Filtros por estado: Todos, Pendientes, En Tránsito, Entregados. |
| `<main> → <section id="kpiSection">` | Total Envíos, Vehículos Activos y Paquetes Entregados. |
| `<main> → <section id="enviosGrid">` | Rejilla adaptativa con un `<article>` por envío. |
| `<aside id="panelBitacora">` | Bitácora de auditoría con filtro por rango de fechas, **solo `ROLE_ADMIN`**. |
| `<footer>` | Derechos de autor y enlaces de soporte. |

---

## 7. Sistema de estilos

`styles.css` declara la paleta completa en `:root` (`--primary-color`, `--bg-dark`,
`--bg-card`, `--text-main`, `--text-muted`, `--accent-success`, `--accent-warning`, la
tipografía `Inter`, sombras y radios).

* **CSS Grid** para la rejilla de envíos y los indicadores, con
  `repeat(auto-fit, minmax(280px, 1fr))`.
* **Flexbox** para centrar el formulario de acceso, alinear la barra de navegación y
  distribuir el contenido interno de cada tarjeta.
* **Mobile-first**: una sola columna por defecto; a partir de `769px` el `<aside>` se
  convierte en columna lateral. Bajo `768px` la navegación y el panel lateral se apilan
  verticalmente sin desbordar la pantalla.

---

## 8. Interactividad y manejo del token

```javascript
const token = sessionStorage.getItem('jwt_token');
const respuesta = await fetch(`${API_ENVIOS}/optimizados`, {
    method: 'GET',
    headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
    }
});
```

* El login guarda el token en `sessionStorage` bajo la clave `jwt_token` y redirige a
  `dashboard.html`.
* `fetchWithAuth()` adjunta el encabezado `Authorization` en **cada** petición asíncrona.
* Ante un **401** o un **403** limpia el almacenamiento y devuelve a `index.html` mostrando
  el motivo en la caja accesible.
* Ante un **400** lee la lista de errores del JSON **RFC 7807** y la despliega en la caja de
  alerta del tablero.

### Formato de error (RFC 7807)

Toda la API responde `application/problem+json`:

```json
{
  "type": "urn:expresofast:error:validacion",
  "title": "Datos de entrada invalidos",
  "status": 400,
  "detail": "Los datos enviados no superaron la validacion",
  "instance": "/api/envios",
  "errores": {
    "codigoRastreo": "Formato invalido. Ejemplo: EXP-1234",
    "pesoKg": "El peso debe ser mayor a cero"
  }
}
```

---

## 9. Matriz de permisos (RBAC)

| Endpoint | Método | Roles permitidos |
|---|---|---|
| `/api/auth/login` | `POST` | **Público** |
| `/api/envios/optimizados` | `GET` | `ADMIN`, `OPERADOR`, `CONDUCTOR` |
| `/api/envios/resumen` | `GET` | `ADMIN`, `OPERADOR`, `CONDUCTOR` |
| `/api/envios` | `POST` | `ADMIN`, `OPERADOR` |
| `/api/envios/{id}/estado` | `PUT` / `PATCH` | `ADMIN` (todas), `OPERADOR` (`EN_TRANSITO`), `CONDUCTOR` (`ENTREGADO`) |
| `/api/envios/{id}/bitacora` | `GET` | `ADMIN`, `OPERADOR` |
| `/api/vehiculos/**` | `ALL` | `ADMIN` |

La regla se declara dos veces a propósito: en el `SecurityFilterChain` por ruta y método, y
con `@PreAuthorize` en el controlador, que sí alcanza a ver el cuerpo de la petición y por
eso puede repartir cada transición entre los roles.

El ocultamiento de botones en el cliente es una mejora de experiencia de usuario; quien
autoriza de verdad es siempre la API.

---

## 10. Reglas de negocio

Secuencia de estados válida:

```
PENDIENTE ──► EN_TRANSITO ──► ENTREGADO
    │                │
    └────────────────┴────► CANCELADO
```

`ENTREGADO` y `CANCELADO` son estados finales. Cualquier violación lanza
`InvalidStateTransitionException`, que se traduce en un **400** con el mensaje
`Transición de estado no permitida para el envío EXP-9003`.

Se conservan además las reglas de las entregas anteriores: código de rastreo único, vehículo
fuera de `MANTENIMIENTO` y validación de capacidad del vehículo.

---

## 11. Pruebas

* **Backend:** `cd backend && mvn test` — 83 pruebas de JUnit 5 y Mockito sobre servicios y
  controladores.
* **API:** importe `docs/ExpresoFast_Postman_Collection.json`. Cubre los casos positivos de
  la matriz y los negativos de **401**, **403**, **400** por validación y **400** por
  transición inválida. Ejecute primero *Autenticacion → 01 Login ADMIN*.
* **Cliente:** en DevTools → *Network* se observa el encabezado
  `Authorization: Bearer <token>` en cada llamada, y en *Application → Session Storage* la
  clave `jwt_token`.

---

## 12. Entrega

* Repositorio GitHub con las carpetas `backend/` y `frontend/`.
* En Mediación Virtual se adjunta un PDF con el enlace al repositorio, capturas del login y
  del dashboard en escritorio y en móvil, y capturas de DevTools mostrando la cabecera
  `Authorization: Bearer <token>`.
