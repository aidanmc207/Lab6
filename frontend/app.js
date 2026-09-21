// ExpresoFast · Consola de Operación Logística · consumo asíncrono de la API REST
'use strict';

const API_BASE = 'http://localhost:8080/api';
const API_AUTH = `${API_BASE}/auth`;
const API_ENVIOS = `${API_BASE}/envios`;

// Claves de sesión. El token vive en sessionStorage: se borra al cerrar la pestaña.
const CLAVE_TOKEN = 'jwt_token';
const CLAVE_PERFIL = 'jwt_perfil';

const VISTA_ACCESO = 'index.html';
const VISTA_CONSOLA = 'dashboard.html';

const ROL_ADMIN = 'ROLE_ADMIN';
const ROL_OPERADOR = 'ROLE_OPERADOR';
const ROL_CONDUCTOR = 'ROLE_CONDUCTOR';

const estadoApp = {
    envios: [],
    bitacora: [],
    filtro: 'TODOS',
    perfil: null
};

const $ = (selector) => document.querySelector(selector);
const $$ = (selector) => Array.from(document.querySelectorAll(selector));

// 1. Sesión

function guardarSesion(respuesta) {
    sessionStorage.setItem(CLAVE_TOKEN, respuesta.token);
    sessionStorage.setItem(CLAVE_PERFIL, JSON.stringify({
        username: respuesta.username,
        nombreCompleto: respuesta.nombreCompleto || respuesta.username,
        roles: respuesta.roles || [],
        conductorId: respuesta.conductorId ?? null
    }));
}

function leerPerfil() {
    try {
        return JSON.parse(sessionStorage.getItem(CLAVE_PERFIL)) || null;
    } catch {
        return null;
    }
}

const tieneRol = (...roles) =>
    roles.some((rol) => (estadoApp.perfil?.roles || []).includes(rol));

/** Oculta cada bloque data-rol que no corresponda al token; la autorizacion real la aplica la API. */
function aplicarRoles() {
    $('#usuarioRoles').innerHTML = (estadoApp.perfil.roles || [])
        .map((rol) => `<span class="rol-etiqueta">${escapar(rol.replace('ROLE_', ''))}</span>`)
        .join('');

    $$('[data-rol]').forEach((elemento) => {
        const permitidos = elemento.dataset.rol.split(/\s+/).filter(Boolean);
        elemento.hidden = !tieneRol(...permitidos);
    });
}

/** Limpia el almacenamiento y devuelve a la pantalla de acceso. */
function cerrarSesion(motivo) {
    sessionStorage.removeItem(CLAVE_TOKEN);
    sessionStorage.removeItem(CLAVE_PERFIL);
    window.location.replace(motivo
        ? `${VISTA_ACCESO}?motivo=${encodeURIComponent(motivo)}`
        : VISTA_ACCESO);
}

// 2. Utilidades de presentación

const formateadorMoneda = new Intl.NumberFormat('es-CR', {
    style: 'currency',
    currency: 'CRC',
    minimumFractionDigits: 2
});

function formatearMoneda(valor) {
    return valor === null || valor === undefined ? '—' : formateadorMoneda.format(Number(valor));
}

function formatearFecha(iso) {
    if (!iso) return '—';
    const fecha = new Date(iso);
    if (Number.isNaN(fecha.getTime())) return '—';
    return fecha.toLocaleString('es-CR', {
        day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit'
    });
}

/** Escapa el texto del servidor antes de inyectarlo en el DOM. */
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
    const toast = $('#toast');
    if (!toast) return;
    toast.textContent = mensaje;
    toast.className = `toast ${tipo}`;
    toast.hidden = false;
    clearTimeout(temporizadorToast);
    temporizadorToast = setTimeout(() => { toast.hidden = true; }, 4200);
}

// 3. Errores RFC 7807

/** Convierte un problem+json en { titulo, detalles[] } listo para pintar. */
function interpretarProblema(cuerpo, status) {
    if (!cuerpo) {
        return { titulo: `Error HTTP ${status}`, detalles: [] };
    }

    const titulo = cuerpo.detail || cuerpo.title || cuerpo.mensaje || `Error HTTP ${status}`;
    const detalles = cuerpo.errores
        ? Object.entries(cuerpo.errores).map(([campo, mensaje]) => `${campo}: ${mensaje}`)
        : [];

    return { titulo, detalles };
}

/** Caja de alerta visual con la lista de errores de validación. */
function mostrarAlerta(problema) {
    const caja = $('#alertaErrores');
    if (!caja) return;

    const lista = problema.detalles.length
        ? `<ul>${problema.detalles.map((d) => `<li>${escapar(d)}</li>`).join('')}</ul>`
        : '';

    caja.innerHTML = `<strong>${escapar(problema.titulo)}</strong>${lista}`;
    caja.hidden = false;
}

function limpiarAlerta() {
    const caja = $('#alertaErrores');
    if (caja) {
        caja.hidden = true;
        caja.innerHTML = '';
    }
}

// 4. Consumo de la API protegida

/** Adjunta Authorization en cada petición; ante 401 o 403 limpia la sesión y vuelve a index.html. */
async function fetchWithAuth(url, opciones = {}) {
    const token = sessionStorage.getItem(CLAVE_TOKEN);

    const respuesta = await fetch(url, {
        ...opciones,
        headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json',
            'Authorization': `Bearer ${token}`,
            ...(opciones.headers || {})
        }
    });

    if (respuesta.status === 401) {
        cerrarSesion('Su sesión expiró o el token no es válido. Vuelva a iniciar sesión.');
        throw new Error('Sesión no válida');
    }

    if (respuesta.status === 403) {
        cerrarSesion('Su rol no tiene permisos para la operación solicitada.');
        throw new Error('Acceso denegado');
    }

    const texto = await respuesta.text();
    const cuerpo = texto ? JSON.parse(texto) : null;

    if (!respuesta.ok) {
        const problema = interpretarProblema(cuerpo, respuesta.status);
        const error = new Error(problema.titulo);
        error.problema = problema;
        throw error;
    }

    return cuerpo;
}

// 5. Vista de acceso (index.html)

function iniciarAcceso() {
    const formulario = $('#loginForm');
    const caja = $('#loginError');
    const boton = $('#btnIngresar');

    const motivo = new URLSearchParams(window.location.search).get('motivo');
    if (motivo) {
        caja.textContent = motivo;
        caja.hidden = false;
    }

    formulario.addEventListener('submit', async (evento) => {
        evento.preventDefault();
        caja.hidden = true;

        if (!formulario.reportValidity()) return;

        boton.disabled = true;
        boton.textContent = 'Validando…';

        try {
            const respuesta = await fetch(`${API_AUTH}/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
                body: JSON.stringify({
                    username: $('#username').value.trim(),
                    password: $('#password').value
                })
            });

            const texto = await respuesta.text();
            const cuerpo = texto ? JSON.parse(texto) : null;

            if (!respuesta.ok) {
                throw new Error(interpretarProblema(cuerpo, respuesta.status).titulo);
            }

            guardarSesion(cuerpo);
            window.location.href = VISTA_CONSOLA;
        } catch (error) {
            caja.textContent = error instanceof TypeError
                ? 'No se pudo contactar la API en http://localhost:8080. Verifique que el backend esté ejecutándose.'
                : error.message;
            caja.hidden = false;
            $('#password').value = '';
        } finally {
            boton.disabled = false;
            boton.textContent = 'Ingresar';
        }
    });
}

// 6. Consola de operaciones (dashboard.html)

function enviosVisibles() {
    return estadoApp.envios.filter((envio) => {
        // El conductor solo ve los envios asignados a su propia ficha.
        if (tieneRol(ROL_CONDUCTOR) && !tieneRol(ROL_ADMIN, ROL_OPERADOR)
                && envio.conductorId !== estadoApp.perfil.conductorId) {
            return false;
        }
        return estadoApp.filtro === 'TODOS' || envio.estadoEnvio === estadoApp.filtro;
    });
}

/** Acciones que cada rol puede ejecutar sobre la tarjeta, segun su estado. */
function accionesDe(envio) {
    const acciones = [];
    const pendiente = envio.estadoEnvio === 'PENDIENTE';
    const enTransito = envio.estadoEnvio === 'EN_TRANSITO';

    if (tieneRol(ROL_ADMIN, ROL_OPERADOR) && pendiente) {
        acciones.push({ estado: 'EN_TRANSITO', texto: 'Despachar' });
    }
    if (tieneRol(ROL_ADMIN, ROL_CONDUCTOR) && enTransito) {
        acciones.push({ estado: 'ENTREGADO', texto: 'Marcar entregado' });
    }
    if (tieneRol(ROL_ADMIN) && (pendiente || enTransito)) {
        acciones.push({ estado: 'CANCELADO', texto: 'Cancelar' });
    }

    const botones = acciones.map((accion) =>
        `<button type="button" class="boton-mini" data-accion="${accion.estado}"
                 data-id="${envio.id}">${accion.texto}</button>`);

    if (tieneRol(ROL_ADMIN)) {
        botones.push(`<button type="button" class="boton-mini" data-bitacora="${envio.id}">
                          Ver bitácora
                      </button>`);
    }

    return botones.join('');
}

function plantillaEnvio(envio) {
    return `
    <article class="envio-tarjeta" data-id="${envio.id}"
             aria-label="Envío ${escapar(envio.codigoRastreo)}">
        <header class="envio-encabezado">
            <span>
                <span class="envio-codigo">${escapar(envio.codigoRastreo)}</span>
                <span class="envio-destino">${escapar(envio.direccionDestino)}</span>
            </span>
            <span class="estado ${escapar(envio.estadoEnvio)}">${escapar(envio.estadoEnvio.replace('_', ' '))}</span>
        </header>

        <div class="envio-datos">
            <div>
                <span class="dato-etiqueta">Peso</span>
                <span class="dato-valor">${Number(envio.pesoKg).toFixed(2)} kg</span>
            </div>
            <div>
                <span class="dato-etiqueta">Costo</span>
                <span class="dato-valor">${formatearMoneda(envio.costo)}</span>
            </div>
            <div>
                <span class="dato-etiqueta">Vehículo</span>
                <span class="dato-valor">${escapar(envio.placaVehiculo) || '—'}</span>
            </div>
            <div>
                <span class="dato-etiqueta">Conductor</span>
                <span class="dato-valor">${escapar(envio.nombreConductor) || '—'}</span>
            </div>
        </div>

        <footer class="envio-acciones">${accionesDe(envio)}</footer>
    </article>`;
}

function renderizar() {
    const rejilla = $('#enviosGrid');
    const titulo = rejilla.querySelector('.titulo-seccion');
    const visibles = enviosVisibles();

    rejilla.innerHTML = '';
    rejilla.appendChild(titulo);
    rejilla.insertAdjacentHTML('beforeend', visibles.map(plantillaEnvio).join(''));
    rejilla.setAttribute('aria-busy', 'false');

    $('#mensajeVacio').hidden = visibles.length > 0;
    $('#resumenListado').textContent =
        `${visibles.length} de ${estadoApp.envios.length} envíos · filtro: ${estadoApp.filtro}`;
}

function mostrarEsqueletos(cantidad = 6) {
    const rejilla = $('#enviosGrid');
    const titulo = rejilla.querySelector('.titulo-seccion');
    rejilla.innerHTML = '';
    rejilla.appendChild(titulo);
    rejilla.insertAdjacentHTML('beforeend',
        Array.from({ length: cantidad }, () => '<p class="esqueleto" aria-hidden="true"></p>').join(''));
    rejilla.setAttribute('aria-busy', 'true');
    $('#mensajeVacio').hidden = true;
}

async function cargarEnvios() {
    mostrarEsqueletos();
    try {
        estadoApp.envios = await fetchWithAuth(`${API_ENVIOS}/optimizados`);
        limpiarAlerta();
        renderizar();
    } catch (error) {
        $('#enviosGrid').setAttribute('aria-busy', 'false');
        mostrarAlerta(error.problema || { titulo: error.message, detalles: [] });
    }
}

async function cargarIndicadores() {
    try {
        const resumen = await fetchWithAuth(`${API_ENVIOS}/resumen`);
        $('#kpiTotal').textContent = resumen.total;
        $('#kpiVehiculos').textContent = resumen.vehiculosActivos;
        $('#kpiEntregados').textContent = resumen.entregados;
    } catch (error) {
        console.warn('Indicadores no disponibles:', error.message);
    }
}

// 7. Acciones sobre un envío (PUT /api/envios/{id}/estado)

async function cambiarEstado(id, nuevoEstado, boton) {
    boton.disabled = true;
    limpiarAlerta();

    try {
        const actualizado = await fetchWithAuth(`${API_ENVIOS}/${id}/estado`, {
            method: 'PUT',
            body: JSON.stringify({
                nuevoEstado,
                observaciones: `Cambio aplicado desde la consola por ${estadoApp.perfil.username}`
            })
        });

        const indice = estadoApp.envios.findIndex((envio) => envio.id === actualizado.id);
        if (indice !== -1) estadoApp.envios[indice] = actualizado;

        renderizar();
        cargarIndicadores();
        notificar(`${actualizado.codigoRastreo} → ${actualizado.estadoEnvio}`, 'exito');
    } catch (error) {
        boton.disabled = false;
        mostrarAlerta(error.problema || { titulo: error.message, detalles: [] });
        notificar(error.message, 'error');
    }
}

function conectarAcciones() {
    $('#enviosGrid').addEventListener('click', (evento) => {
        const accion = evento.target.closest('[data-accion]');
        if (accion) {
            cambiarEstado(Number(accion.dataset.id), accion.dataset.accion, accion);
            return;
        }

        const bitacora = evento.target.closest('[data-bitacora]');
        if (bitacora) cargarBitacora(Number(bitacora.dataset.bitacora));
    });
}

// 8. Bitácora de auditoría (aside, solo ROLE_ADMIN)

function filasBitacoraVisibles() {
    const desde = $('#bitacoraDesde').value;
    const hasta = $('#bitacoraHasta').value;

    return estadoApp.bitacora.filter((fila) => {
        if (!fila.fechaCambio) return false;
        const dia = fila.fechaCambio.slice(0, 10);
        return (!desde || dia >= desde) && (!hasta || dia <= hasta);
    });
}

function renderizarBitacora() {
    const lista = $('#bitacoraLista');
    const filas = filasBitacoraVisibles();

    if (filas.length === 0) {
        lista.innerHTML = '<li class="texto-apagado">Sin movimientos en el rango seleccionado.</li>';
        return;
    }

    lista.innerHTML = filas.map((fila) => `
        <li class="bitacora-item">
            <p class="bitacora-transicion">
                <span class="estado ${escapar(fila.estadoAnterior)}">${escapar(String(fila.estadoAnterior).replace('_', ' '))}</span>
                <span aria-hidden="true">→</span>
                <span class="estado ${escapar(fila.estadoNuevo)}">${escapar(String(fila.estadoNuevo).replace('_', ' '))}</span>
            </p>
            <p class="bitacora-meta">${formatearFecha(fila.fechaCambio)} · ${escapar(fila.usuario)}</p>
            <p class="bitacora-observacion">${escapar(fila.observaciones) || 'Sin observaciones'}</p>
        </li>`).join('');
}

async function cargarBitacora(envioId) {
    const envio = estadoApp.envios.find((e) => e.id === envioId);
    $('#bitacoraContexto').textContent = envio
        ? `${envio.codigoRastreo} · ${envio.direccionDestino}`
        : `Envío #${envioId}`;
    $('#bitacoraLista').innerHTML = '<li class="texto-apagado">Cargando historial…</li>';

    try {
        estadoApp.bitacora = await fetchWithAuth(`${API_ENVIOS}/${envioId}/bitacora`);
        renderizarBitacora();
        $('#panelBitacora').scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    } catch (error) {
        $('#bitacoraLista').innerHTML =
            `<li class="texto-apagado">No se pudo cargar la bitácora: ${escapar(error.message)}</li>`;
    }
}

function conectarBitacora() {
    ['#bitacoraDesde', '#bitacoraHasta'].forEach((selector) => {
        $(selector).addEventListener('change', renderizarBitacora);
    });
    // El reset limpia los campos despues del evento: se difiere el repintado.
    $('#formFiltroBitacora').addEventListener('reset', () => setTimeout(renderizarBitacora, 0));
}

// 9. Alta de vehículos (solo ROLE_ADMIN)

function conectarDialogoVehiculo() {
    const dialogo = $('#dialogoVehiculo');
    const formulario = $('#formVehiculo');

    $('#btnNuevoVehiculo').addEventListener('click', async () => {
        try {
            const empresas = await fetchWithAuth(`${API_BASE}/catalogos/empresas`);
            $('#empresaId').innerHTML = empresas
                .map((empresa) => `<option value="${empresa.id}">${escapar(empresa.nombre)}</option>`)
                .join('');
        } catch (error) {
            console.warn('Catálogo de empresas no disponible:', error.message);
        }
        dialogo.showModal();
    });

    $('#btnCerrarDialogo').addEventListener('click', () => dialogo.close());

    $('#btnGuardarVehiculo').addEventListener('click', async () => {
        if (!formulario.reportValidity()) return;
        limpiarAlerta();

        try {
            const creado = await fetchWithAuth(`${API_BASE}/vehiculos`, {
                method: 'POST',
                body: JSON.stringify({
                    placa: $('#placa').value.trim().toUpperCase(),
                    capacidadKg: parseFloat($('#capacidadKg').value),
                    estado: $('#estadoVehiculo').value,
                    empresaId: Number($('#empresaId').value)
                })
            });

            dialogo.close();
            formulario.reset();
            notificar(`Vehículo ${creado.placa} registrado.`, 'exito');
            cargarIndicadores();
        } catch (error) {
            dialogo.close();
            mostrarAlerta(error.problema || { titulo: error.message, detalles: [] });
        }
    });
}

function conectarFiltros() {
    $$('.filtro').forEach((boton) => {
        boton.addEventListener('click', () => {
            estadoApp.filtro = boton.dataset.estado;
            $$('.filtro').forEach((otro) => {
                const activo = otro === boton;
                otro.classList.toggle('activo', activo);
                otro.setAttribute('aria-pressed', String(activo));
            });
            renderizar();
        });
    });
}

async function iniciarConsola() {
    if (!sessionStorage.getItem(CLAVE_TOKEN)) {
        cerrarSesion('Debe iniciar sesión para entrar a la consola.');
        return;
    }

    estadoApp.perfil = leerPerfil() || { roles: [] };
    $('#usuarioNombre').textContent = estadoApp.perfil.nombreCompleto || estadoApp.perfil.username;

    aplicarRoles();

    $('#btnLogout').addEventListener('click', () => cerrarSesion());
    conectarFiltros();
    conectarAcciones();

    if (tieneRol(ROL_ADMIN)) {
        conectarBitacora();
        conectarDialogoVehiculo();
    }

    await cargarEnvios();
    await cargarIndicadores();
}

// 10. Arranque: cada vista engancha su propia lógica

document.addEventListener('DOMContentLoaded', () => {
    if ($('#loginForm')) {
        iniciarAcceso();
    } else if ($('#enviosGrid')) {
        iniciarConsola();
    }
});
