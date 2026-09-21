# ExpresoFast — Laboratorio 7: Suite de Pruebas Unitarias e Integración

Curso IF0009 — Desarrollo de Software IV · Universidad de Costa Rica, Sede del Atlántico, Recinto de Paraíso.

Tercera parte del proyecto ExpresoFast. Sobre la plataforma construida en los laboratorios 5 y 6
(modelo relacional, JPQL con `JOIN FETCH`, DTOs, JWT y RBAC) se agrega una suite automatizada de
pruebas unitarias y de corte de controlador, con certificación de cobertura mediante JaCoCo.

## Requisitos

- Java 21 (Temurin 21.0.6 o equivalente)
- Apache Maven 3.9.x
- Microsoft SQL Server (solo para ejecutar la aplicación; **la suite de pruebas no requiere base de datos**)

## Ejecutar las pruebas

Desde la carpeta `backend/`:

```bash
mvn clean test
```

Ejecuta las 83 pruebas de la suite mediante `maven-surefire-plugin` 3.2.5 y genera el reporte de
cobertura de JaCoCo en la fase `test`.

## Ejecutar pruebas + verificación de cobertura

```bash
mvn clean verify
```

Además de las pruebas, ejecuta `jacoco:check`: la construcción **falla** si la cobertura de
instrucciones del paquete de servicios es menor al **85 %**.

## Ver el reporte de cobertura

Abra en el navegador:

```
backend/target/site/jacoco/index.html
```

En Windows:

```bash
start backend/target/site/jacoco/index.html
```

## Estructura de la suite

| Clase de prueba | Tipo | Qué certifica |
|---|---|---|
| `EnvioServiceTest` | JUnit 5 + Mockito | Registro de envíos (estado inicial `PENDIENTE`), capacidad del vehículo, transiciones de estado, bitácora, actualización masiva y cálculo de tarifas |
| `VehiculoServiceTest` | JUnit 5 + Mockito | Alta de vehículos, placa duplicada, empresa inexistente y eliminación con envíos asociados |
| `EmpresaLogisticaServiceTest` | JUnit 5 + Mockito | Registro y consulta de empresas, nombre y cédula jurídica duplicados |
| `CatalogoServiceTest` | JUnit 5 + Mockito | Catálogos de vehículos, conductores y empresas |
| `AuthServiceTest` | JUnit 5 + Mockito | Emisión del token JWT, credenciales incorrectas y consulta de perfil |
| `EnvioControllerTest` | `@WebMvcTest` + MockMvc | Contrato HTTP de `/api/envios`: 200, 201, 400 y 404 con `jsonPath` |
| `AuthControllerTest` | `@WebMvcTest` + MockMvc | Contrato HTTP de `/api/auth/login`: 200 con token, 401 y 400 |

Las pruebas parametrizadas (`@ParameterizedTest` + `@CsvSource`) de `EnvioServiceTest` certifican la
matriz de tarifas de `EnvioService.calcularTarifa(pesoKg, distanciaKm)`:

| Zona | Distancia | Tarifa |
|---|---|---|
| Local | hasta 5 km | ₡120 por kg |
| Ruta nacional | más de 5 km | ₡500 por kg |

Un peso o una distancia menores o iguales a cero lanzan `ReglaNegocioException`.

## Configuración de cobertura

`jacoco-maven-plugin` 0.8.11 con las metas `prepare-agent`, `report` (fase `test`) y `check`.
La regla exige un mínimo de `0.85` de `INSTRUCTION / COVEREDRATIO` sobre el paquete
`cr.ac.ucr.paraiso.ie.c4h845.expresofast.business`, que es la capa de servicios de este proyecto
(equivalente a `com.expresofast.service` en el enunciado).

Resultado obtenido: **99.70 %** de cobertura de instrucciones en la capa de servicios.

## Correspondencia con el enunciado

El enunciado usa nombres genéricos que se mapean así sobre la arquitectura de este proyecto:

| Enunciado | Este proyecto |
|---|---|
| Paquete `com.expresofast.service` | `cr.ac.ucr.paraiso.ie.c4h845.expresofast.business` |
| `CapacidadExcedidaException` | `ReglaNegocioException` (validación de capacidad del vehículo) |
| `DuplicateResourceException` | `ReglaNegocioException` (placa, código de rastreo o nombre duplicados) |
| `VehiculoRepository.existsByPlaca()` | `VehiculoRepository.findByPlaca()` |
| `EmpresaLogisticaService` | Se agregó el servicio; los catálogos siguen en `CatalogoService`, también probado |

Dos reglas del enunciado se ajustaron a la lógica ya implementada en el laboratorio 6, que no se
modificó:

- **Cancelación de envíos**: un envío `EN_TRANSITO` sí puede cancelarse (la matriz de transiciones lo
  permite). La prueba `cancelarEnvio_EnvioEntregado_LanzaExcepcion` verifica el caso que sí es
  inválido: cancelar un envío ya `ENTREGADO`, por ser estado final.
- **Conductor inactivo**: la entidad `Conductor` no tiene estado de actividad. En su lugar se prueba la
  regla equivalente sobre la flota: un vehículo en `MANTENIMIENTO` no puede recibir envíos.

## Estructura del repositorio

```
backend/          Aplicación Spring Boot y suite de pruebas (src/test/java)
database/         Scripts SQL del esquema y semillas
docs/             Colección de Postman
frontend/         Portal estático que consume la API
```
