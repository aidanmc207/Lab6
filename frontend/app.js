// ExpresoFast · Tablero protegido con JWT (Fetch API + RBAC en el cliente)
'use strict';

/** Base de la API de Spring Boot. Cambie el puerto si server.port es distinto. */
const API_BASE = 'http://localhost:8080/api';
const API_ENVIOS = `${API_BASE}/envios`;
const API_CATALOGOS = `${API_BASE}/catalogos`;

// Claves de sesion en localStorage (las escribe login.html)
const CLAVE_TOKEN = 'jwt_token';
const CLAVE_USUARIO = 'jwt_username';
const CLAVE_NOMBRE = 'jwt_nombre';
const CLAVE_ROLES = 'jwt_roles';
const CLAVE_EXPIRA = 'jwt_expira';

const ROL_ADMIN = 'ROLE_ADMIN';
const ROL_OPERADOR = 'ROLE_OPERADOR';
const ROL_CONDUCTOR = 'ROLE_CONDUCTOR';

// Estado local
const estadoApp = {
    envios: [],        // última respuesta de GET /api/envios/optimizados
    filtro: 'TODOS',   // filtro de estado activo en el panel lateral
    busqueda: '',      // texto del buscador
    roles: [],         // roles del usuario autenticado (claim del JWT)
    bitacora: [],      // historial cargado en el modal
    envioBitacora: null
};

// Referencias
const $ = (selector) => document.querySelector(selector);
const $$ = (selector) => Array.from(document.querySelectorAll(selector));

const tablero = $('#tableroEnvios');
const mensajeVacio = $('#mensajeVacio');
const resumenListado = $('#resumenListado');
const apiEstado = $('#apiEstado');
const toast = $('#toast');
const modal = $('#modalBitacora');

// 1. Sesión: token, roles y cierre de sesión

function obtenerToken() {
    return localStorage.getItem(CLAVE_TOKEN);
}

function obtenerRoles() {
    try {
        const crudo = localStorage.getItem(CLAVE_ROLES);
        const roles = crudo ? JSON.parse(crudo) : [];
        return Array.isArray(roles) ? roles : [];
    } catch {
        return [];
    }
}

const tieneRol = (...roles) => roles.some((rol) => estadoApp.roles.includes(rol));

/** Borra la sesión y devuelve al login. Se usa también ante un 401/403. */
function cerrarSesion(motivo) {
    [CLAVE_TOKEN, CLAVE_USUARIO, CLAVE_NOMBRE, CLAVE_ROLES, CLAVE_EXPIRA]
        .forEach((clave) => localStorage.removeItem(clave));

    const destino = motivo
        ? `login.html?motivo=${encodeURIComponent(motivo)}`
        : 'login.html';
    window.location.replace(destino);
}

/** Guardia de ruta: sin token vigente no se pinta el tablero. */
function exigirSesion() {
    const token = obtenerToken();
    const expira = localStorage.getItem(CLAVE_EXPIRA);

    if (!token) {
        cerrarSesion('Debe iniciar sesión para consultar el tablero.');
        return false;
    }
    if (expira && new Date(expira) <= new Date()) {
        cerrarSesion('Su sesión expiró. Vuelva a iniciar sesión.');
        return false;
    }

    estadoApp.roles = obtenerRoles();
    return true;
}

// 2. Utilidades

const formateadorMoneda = new Intl.NumberFormat('es-CR', {
    style: 'currency',
    currency: 'CRC',
    minimumFractionDigits: 2
});

function formatearMoneda(valor) {
    if (valor === null || valor === undefined) return '—';
    return formateadorMoneda.format(Number(valor));
}

function formatearFecha(iso) {
    if (!iso) return '—';
    const fecha = new Date(iso);
    if (Number.isNaN(fecha.getTime())) return '—';
    return fecha.toLocaleString('es-CR', {
        day: '2-digit', month: '2-digit', year: 'numeric',
        hour: '2-digit', minute: '2-digit'
    });
}

/** Escapa el texto que proviene del servidor antes de inyectarlo en el DOM. */
function escapar(texto) {
    if (texto === null || texto === undefined) return '';
    return String(texto)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

let temporizadorToast = null;

function notificar(mensaje, tipo = 'info') {
    toast.textContent = mensaje;
    toast.className = `toast ${tipo}`;
    toast.hidden = false;
    clearTimeout(temporizadorToast);
    temporizadorToast = setTimeout(() => { toast.hidden = true; }, 4200);
}

function marcarApi(ok, detalle) {
    apiEstado.textContent = ok ? 'API conectada' : `API sin conexión${detalle ? ': ' + detalle : ''}`;
    apiEstado.className = `api-estado ${ok ? 'ok' : 'error'}`;
}

// 3. Interceptor de peticiones: adjunta el token y vigila la expiración

async function fetchWithAuth(url, opciones = {}) {
    const respuesta = await fetch(url, {
        ...opciones,
        headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json',
            'Authorization': `Bearer ${localStorage.getItem(CLAVE_TOKEN)}`,
            ...(opciones.headers || {})
        }
    });

    if (respuesta.status === 401) {
        cerrarSesion('Su sesión expiró o el token no es válido. Inicie sesión nuevamente.');
        throw new Error('Sesión no válida');
    }

    if (respuesta.status === 403) {
        // Rol insuficiente: no se destruye la sesión, solo se informa.
        const detalle = await leerMensaje(respuesta);
        throw new Error(detalle || 'Su rol no tiene permisos para esta operación');
    }

    const texto = await respuesta.text();
    const cuerpo = texto ? JSON.parse(texto) : null;

    if (!respuesta.ok) {
        throw new Error(mensajeDeError(cuerpo) || `HTTP ${respuesta.status}`);
    }
    return cuerpo;
}

/** Arma el mensaje mostrable a partir del ErrorResponseDTO del backend. */
function mensajeDeError(cuerpo) {
    if (!cuerpo) return null;

    if (cuerpo.errores && typeof cuerpo.errores === 'object') {
        const detalle = Object.entries(cuerpo.errores)
            .map(([campo, mensaje]) => `${campo}: ${mensaje}`)
            .join(' · ');
        return detalle ? `${cuerpo.mensaje}. ${detalle}` : cuerpo.mensaje;
    }
    return cuerpo.mensaje || cuerpo.message || null;
}

async function leerMensaje(respuesta) {
    try {
        const texto = await respuesta.text();
        return texto ? mensajeDeError(JSON.parse(texto)) : null;
    } catch {
        return null;
    }
}

// 4. Renderizado condicional por rol

function aplicarRoles() {
    $('#usuarioNombre').textContent =
        localStorage.getItem(CLAVE_NOMBRE) || localStorage.getItem(CLAVE_USUARIO) || '—';

    $('#usuarioRoles').innerHTML = estadoApp.roles
        .map((rol) => `<span class="rol-badge">${escapar(rol.replace('ROLE_', ''))}</span>`)
        .join('');

    $$('[data-rol]').forEach((elemento) => {
        const permitidos = elemento.dataset.rol.split(/\s+/).filter(Boolean);
        elemento.hidden = !tieneRol(...permitidos);
    });
}

// 5. GET · Carga de envíos con la consulta optimizada (JOIN FETCH)

function mostrarEsqueletos(cantidad = 6) {
    tablero.setAttribute('aria-busy', 'true');
    tablero.innerHTML = Array.from({ length: cantidad },
        () => '<div class="skeleton" aria-hidden="true"></div>').join('');
    mensajeVacio.hidden = true;
}

async function cargarEnvios() {
    mostrarEsqueletos();
    resumenListado.textContent = 'Cargando…';

    try {
        // Un único viaje a SQL Server gracias al JOIN FETCH del EnvioRepository.
        estadoApp.envios = await fetchWithAuth(`${API_ENVIOS}/optimizados`);
        marcarApi(true);
        renderizar();
        actualizarContadores();
    } catch (error) {
        marcarApi(false, error.message);
        tablero.innerHTML = '';
        tablero.setAttribute('aria-busy', 'false');
        mensajeVacio.hidden = false;
        mensajeVacio.textContent =
            `No se pudo cargar la lista de envíos (${error.message}). ` +
            'Verifique que el backend esté ejecutándose en ' + API_BASE + '.';
        resumenListado.textContent = 'Sin datos';
        notificar(`Error al consultar la API: ${error.message}`, 'error');
    }
}

/** KPIs desde el endpoint de resumen, con respaldo en el conteo local. */
async function cargarResumen() {
    try {
        const resumen = await fetchWithAuth(`${API_ENVIOS}/resumen`);
        $('#kpiTotal').textContent = resumen.total;
        $('#kpiPendiente').textContent = resumen.pendientes;
        $('#kpiTransito').textContent = resumen.enTransito;
        $('#kpiEntregado').textContent = resumen.entregados;
    } catch (error) {
        console.warn('Resumen no disponible, se calcula localmente:', error.message);
        const contar = (estado) => estadoApp.envios.filter((e) => e.estadoEnvio === estado).length;
        $('#kpiTotal').textContent = estadoApp.envios.length;
        $('#kpiPendiente').textContent = contar('PENDIENTE');
        $('#kpiTransito').textContent = contar('EN_TRANSITO');
        $('#kpiEntregado').textContent = contar('ENTREGADO');
    }
}

/** Catálogos para los datalist de vehículos y conductores. */
async function cargarCatalogos() {
    try {
        const [vehiculos, conductores] = await Promise.all([
            fetchWithAuth(`${API_CATALOGOS}/vehiculos`),
            fetchWithAuth(`${API_CATALOGOS}/conductores`)
        ]);

        $('#listaVehiculos').innerHTML = vehiculos.map((v) =>
            `<option value="${v.id}" label="${escapar(v.placa)} · ${v.capacidadKg} kg · ${escapar(v.estado)}"></option>`
        ).join('');

        $('#listaConductores').innerHTML = conductores.map((c) =>
            `<option value="${c.id}" label="${escapar(c.nombreCompleto)} · ${escapar(c.licencia)}"></option>`
        ).join('');
    } catch (error) {
        console.warn('Catálogos no disponibles:', error.message);
    }
}

// 6. Render del tablero (sin recargar la página)

function enviosVisibles() {
    const texto = estadoApp.busqueda.trim().toLowerCase();

    return estadoApp.envios.filter((envio) => {
        const coincideEstado = estadoApp.filtro === 'TODOS' || envio.estadoEnvio === estadoApp.filtro;
        if (!coincideEstado) return false;
        if (!texto) return true;

        return [envio.codigoRastreo, envio.direccionDestino, envio.placaVehiculo, envio.nombreConductor]
            .filter(Boolean)
            .some((campo) => campo.toLowerCase().includes(texto));
    });
}

function plantillaEnvio(envio) {
    const finalizado = envio.estadoEnvio === 'ENTREGADO' || envio.estadoEnvio === 'CANCELADO';

    // PATCH /api/envios/{id}/estado -> ROLE_ADMIN o ROLE_CONDUCTOR.
    const acciones = tieneRol(ROL_ADMIN, ROL_CONDUCTOR) ? `
        <button type="button" class="btn-mini" data-accion="EN_TRANSITO" data-id="${envio.id}"
                ${finalizado || envio.estadoEnvio !== 'PENDIENTE' ? 'disabled' : ''}>
            Marcar en tránsito
        </button>
        <button type="button" class="btn-mini" data-accion="ENTREGADO" data-id="${envio.id}"
                ${finalizado || envio.estadoEnvio !== 'EN_TRANSITO' ? 'disabled' : ''}>
            Marcar entregado
        </button>
        <button type="button" class="btn-mini" data-accion="CANCELADO" data-id="${envio.id}"
                ${finalizado ? 'disabled' : ''}>
            Cancelar
        </button>` : '';

    // GET /api/envios/{id}/bitacora -> ROLE_ADMIN o ROLE_OPERADOR.
    const bitacora = tieneRol(ROL_ADMIN, ROL_OPERADOR)
        ? `<button type="button" class="btn-mini btn-mini-acento" data-bitacora="${envio.id}">
               Ver bitácora
           </button>`
        : '';

    return `
    <article class="envio-card" data-id="${envio.id}" aria-label="Envío ${escapar(envio.codigoRastreo)}">
        <header class="envio-head">
            <div>
                <p class="envio-codigo">${escapar(envio.codigoRastreo)}</p>
                <p class="envio-destino">${escapar(envio.direccionDestino)}</p>
            </div>
            <span class="pill-status ${escapar(envio.estadoEnvio)}">${escapar(envio.estadoEnvio.replace('_', ' '))}</span>
        </header>

        <div class="envio-datos">
            <div>
                <span class="dato-label">Peso</span>
                <span class="dato-valor">${Number(envio.pesoKg).toFixed(2)} kg</span>
            </div>
            <div>
                <span class="dato-label">Costo</span>
                <span class="dato-valor">${formatearMoneda(envio.costo)}</span>
            </div>
            <div>
                <span class="dato-label">Vehículo</span>
                <span class="dato-valor">${escapar(envio.placaVehiculo) || '—'}</span>
            </div>
            <div>
                <span class="dato-label">Empresa</span>
                <span class="dato-valor">${escapar(envio.empresaNombre) || '—'}</span>
            </div>
            <div>
                <span class="dato-label">Conductor</span>
                <span class="dato-valor">${escapar(envio.nombreConductor) || '—'}</span>
            </div>
            <div>
                <span class="dato-label">Licencia</span>
                <span class="dato-valor">${escapar(envio.conductorLicencia) || '—'}</span>
            </div>
        </div>

        <footer class="envio-acciones">
            ${acciones}
            ${bitacora}
        </footer>

        <p class="envio-auditoria">
            Creado: ${formatearFecha(envio.fechaCreacion)} · Modificado: ${formatearFecha(envio.fechaModificacion)}
        </p>
    </article>`;
}

function renderizar() {
    const visibles = enviosVisibles();

    tablero.innerHTML = visibles.map(plantillaEnvio).join('');
    tablero.setAttribute('aria-busy', 'false');

    mensajeVacio.hidden = visibles.length > 0;
    if (visibles.length === 0) {
        mensajeVacio.textContent = 'No hay envíos que coincidan con el filtro seleccionado.';
    }

    resumenListado.textContent =
        `${visibles.length} de ${estadoApp.envios.length} envíos · filtro: ${estadoApp.filtro}`;
}

function actualizarContadores() {
    const conteo = { TODOS: estadoApp.envios.length };
    for (const envio of estadoApp.envios) {
        conteo[envio.estadoEnvio] = (conteo[envio.estadoEnvio] || 0) + 1;
    }
    $$('[data-contador]').forEach((span) => {
        span.textContent = conteo[span.dataset.contador] || 0;
    });
}

// 7. POST · Registro de un nuevo envío (ROLE_ADMIN / ROLE_OPERADOR)

$('#formEnvio').addEventListener('submit', async (evento) => {
    evento.preventDefault();
    const formulario = evento.currentTarget;

    if (!formulario.reportValidity()) return;

    const boton = $('#btnGuardar');
    boton.disabled = true;
    boton.textContent = 'Registrando…';

    // Estructura exigida por EnvioRequestDTO: las relaciones viajan como ids planos.
    const payload = {
        codigoRastreo: $('#codigoRastreo').value.trim().toUpperCase(),
        direccionDestino: $('#direccionDestino').value.trim(),
        pesoKg: parseFloat($('#pesoKg').value),
        costo: parseFloat($('#costo').value),
        vehiculoId: parseInt($('#vehiculoId').value, 10),
        conductorId: parseInt($('#conductorId').value, 10)
    };

    try {
        const creado = await fetchWithAuth(API_ENVIOS, {
            method: 'POST',
            body: JSON.stringify(payload)
        });

        formulario.reset();
        notificar(`Envío ${creado.codigoRastreo} registrado correctamente.`, 'exito');
        await Promise.all([cargarEnvios(), cargarResumen()]);
    } catch (error) {
        notificar(error.message, 'error');
    } finally {
        boton.disabled = false;
        boton.textContent = 'Registrar envío';
    }
});

// 8. PATCH · Cambio de estado y apertura de la bitácora

tablero.addEventListener('click', async (evento) => {
    const botonBitacora = evento.target.closest('[data-bitacora]');
    if (botonBitacora) {
        abrirBitacora(parseInt(botonBitacora.dataset.bitacora, 10));
        return;
    }

    const boton = evento.target.closest('[data-accion]');
    if (!boton) return;

    const { id, accion } = boton.dataset;
    const observaciones = prompt(
        `Observaciones para la bitácora (opcional) al pasar el envío a ${accion}:`, '');

    // Cancelar el prompt aborta la operación; una cadena vacía sí continúa.
    if (observaciones === null) return;

    const botones = $$(`[data-id="${id}"][data-accion]`);
    botones.forEach((b) => { b.disabled = true; });

    try {
        const actualizado = await fetchWithAuth(`${API_ENVIOS}/${id}/estado`, {
            method: 'PATCH',
            body: JSON.stringify({
                nuevoEstado: accion,
                observaciones: observaciones.trim() || null
            })
        });

        // Se actualiza el DOM en memoria, sin recargar la página.
        const indice = estadoApp.envios.findIndex((e) => e.id === actualizado.id);
        if (indice !== -1) {
            estadoApp.envios[indice] = actualizado;
        }
        renderizar();
        actualizarContadores();
        cargarResumen();
        notificar(`${actualizado.codigoRastreo} → ${actualizado.estadoEnvio}`, 'exito');
    } catch (error) {
        botones.forEach((b) => { b.disabled = false; });
        notificar(error.message, 'error');
    }
});

// 9. Modal de bitácora de auditoría histórica

async function abrirBitacora(envioId) {
    const envio = estadoApp.envios.find((e) => e.id === envioId);
    estadoApp.envioBitacora = envio || null;
    estadoApp.bitacora = [];

    $('#bitacoraEnvioInfo').textContent = envio
        ? `${envio.codigoRastreo} · ${envio.direccionDestino}`
        : `Envío #${envioId}`;
    $('#formFiltroBitacora').reset();
    $('#bitacoraCuerpo').innerHTML = '<p class="ayuda">Cargando historial…</p>';

    modal.hidden = false;
    document.body.classList.add('sin-scroll');
    $('.btn-cerrar').focus();

    try {
        estadoApp.bitacora = await fetchWithAuth(`${API_ENVIOS}/${envioId}/bitacora`);
        renderizarBitacora();
    } catch (error) {
        $('#bitacoraCuerpo').innerHTML =
            `<p class="vacio">No se pudo cargar la bitácora: ${escapar(error.message)}</p>`;
    }
}

function cerrarBitacora() {
    modal.hidden = true;
    document.body.classList.remove('sin-scroll');
}

/** Reto autónomo: filtra el historial entre una fecha inicial y una final. */
function bitacoraVisible() {
    const desde = $('#bitacoraDesde').value;
    const hasta = $('#bitacoraHasta').value;

    return estadoApp.bitacora.filter((fila) => {
        if (!fila.fechaCambio) return false;
        const dia = fila.fechaCambio.slice(0, 10); // ISO yyyy-MM-dd
        if (desde && dia < desde) return false;
        if (hasta && dia > hasta) return false;
        return true;
    });
}

function renderizarBitacora() {
    const filas = bitacoraVisible();
    const cuerpo = $('#bitacoraCuerpo');

    if (filas.length === 0) {
        cuerpo.innerHTML = estadoApp.bitacora.length === 0
            ? '<p class="vacio">Este envío todavía no registra cambios de estado.</p>'
            : '<p class="vacio">Ningún movimiento cae dentro del rango de fechas seleccionado.</p>';
        return;
    }

    cuerpo.innerHTML = `
        <p class="ayuda">${filas.length} de ${estadoApp.bitacora.length} movimiento(s)</p>
        <ol class="bitacora-lista">
            ${filas.map((fila) => `
                <li class="bitacora-item">
                    <div class="bitacora-transicion">
                        <span class="pill-status ${escapar(fila.estadoAnterior)}">${escapar(String(fila.estadoAnterior).replace('_', ' '))}</span>
                        <span class="bitacora-flecha" aria-hidden="true">→</span>
                        <span class="pill-status ${escapar(fila.estadoNuevo)}">${escapar(String(fila.estadoNuevo).replace('_', ' '))}</span>
                    </div>
                    <p class="bitacora-meta">
                        <strong>${formatearFecha(fila.fechaCambio)}</strong> ·
                        usuario <code>${escapar(fila.usuario)}</code>
                    </p>
                    <p class="bitacora-obs">${escapar(fila.observaciones) || 'Sin observaciones'}</p>
                </li>`).join('')}
        </ol>`;
}

$$('[data-cerrar-modal]').forEach((elemento) => {
    elemento.addEventListener('click', cerrarBitacora);
});

document.addEventListener('keydown', (evento) => {
    if (evento.key === 'Escape' && !modal.hidden) cerrarBitacora();
});

['#bitacoraDesde', '#bitacoraHasta'].forEach((selector) => {
    $(selector).addEventListener('change', renderizarBitacora);
});

$('#formFiltroBitacora').addEventListener('reset', () => {
    // El reset limpia los input después del evento: se difiere el repintado.
    setTimeout(renderizarBitacora, 0);
});

// 10. PATCH masivo por vehículo (@Modifying · solo ROLE_ADMIN)

$('#formMasivo').addEventListener('submit', async (evento) => {
    evento.preventDefault();

    const vehiculoId = parseInt($('#masivoVehiculoId').value, 10);
    const nuevoEstado = $('#masivoEstado').value;

    if (!Number.isInteger(vehiculoId) || vehiculoId < 1) {
        notificar('Indique un ID de vehículo válido.', 'error');
        return;
    }

    if (!confirm(`¿Cambiar a ${nuevoEstado} TODOS los envíos del vehículo ${vehiculoId}?`)) return;

    try {
        const resultado = await fetchWithAuth(`${API_ENVIOS}/vehiculo/${vehiculoId}/estado`, {
            method: 'PATCH',
            body: JSON.stringify({
                nuevoEstado,
                observaciones: $('#masivoObservaciones').value.trim() || null
            })
        });
        notificar(`${resultado.enviosActualizados} envío(s) actualizados a ${resultado.nuevoEstado}.`, 'exito');
        await Promise.all([cargarEnvios(), cargarResumen()]);
    } catch (error) {
        notificar(error.message, 'error');
    }
});

// 11. Filtros, búsqueda, refresco y cierre de sesión

$$('.filtro').forEach((boton) => {
    boton.addEventListener('click', () => {
        estadoApp.filtro = boton.dataset.estado;

        $$('.filtro').forEach((b) => {
            const activo = b === boton;
            b.classList.toggle('activo', activo);
            b.setAttribute('aria-pressed', String(activo));
        });
        renderizar();
    });
});

$('#buscador').addEventListener('input', (evento) => {
    estadoApp.busqueda = evento.target.value;
    renderizar();
});

$('#btnRefrescar').addEventListener('click', () => {
    cargarEnvios();
    cargarResumen();
    if (tieneRol(ROL_ADMIN, ROL_OPERADOR)) cargarCatalogos();
});

$('#btnSalir').addEventListener('click', () => cerrarSesion());

// 12. Arranque

document.addEventListener('DOMContentLoaded', async () => {
    if (!exigirSesion()) return;

    aplicarRoles();
    await cargarEnvios();

    const tareas = [cargarResumen()];
    if (tieneRol(ROL_ADMIN, ROL_OPERADOR)) tareas.push(cargarCatalogos());
    await Promise.all(tareas);
});
