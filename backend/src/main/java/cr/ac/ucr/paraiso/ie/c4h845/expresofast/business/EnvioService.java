package cr.ac.ucr.paraiso.ie.c4h845.expresofast.business;

import cr.ac.ucr.paraiso.ie.c4h845.expresofast.data.BitacoraEnvioRepository;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.data.ConductorRepository;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.data.EnvioRepository;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.data.UsuarioRepository;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.data.VehiculoRepository;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.domain.BitacoraEnvio;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.domain.Conductor;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.domain.Envio;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.domain.Usuario;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.domain.Vehiculo;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.dto.BitacoraResponseDTO;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.dto.CambioEstadoDTO;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.dto.EnvioRequestDTO;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.dto.EnvioResponseDTO;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.dto.ResumenEnviosDTO;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.exception.InvalidStateTransitionException;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.exception.ReglaNegocioException;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.exception.ResourceNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Reglas de negocio, auditoria de estados y demarcacion transaccional. */
@Service
@Transactional(readOnly = true)
public class EnvioService {

    private static final Set<String> ESTADOS_VALIDOS =
            Set.of(Envio.PENDIENTE, Envio.EN_TRANSITO, Envio.ENTREGADO, Envio.CANCELADO);

    /** Estados terminales: una vez alcanzados el envio ya no admite mas transiciones. */
    private static final Set<String> ESTADOS_FINALES =
            Set.of(Envio.ENTREGADO, Envio.CANCELADO);

    /** Secuencia logica permitida (reto autonomo del laboratorio). */
    private static final Map<String, Set<String>> TRANSICIONES_PERMITIDAS = Map.of(
            Envio.PENDIENTE, Set.of(Envio.EN_TRANSITO, Envio.CANCELADO),
            Envio.EN_TRANSITO, Set.of(Envio.ENTREGADO, Envio.CANCELADO),
            Envio.ENTREGADO, Set.of(),
            Envio.CANCELADO, Set.of());

    /** Lab 7: distancia que separa la tarifa local de la tarifa de ruta nacional. */
    private static final double DISTANCIA_LOCAL_KM = 5.0;

    /** Lab 7: colones por kilogramo dentro de la zona local (hasta 5 km). */
    private static final double TARIFA_LOCAL_POR_KG = 120.0;

    /** Lab 7: colones por kilogramo en ruta nacional (mas de 5 km). */
    private static final double TARIFA_NACIONAL_POR_KG = 500.0;

    private final EnvioRepository envioRepository;
    private final VehiculoRepository vehiculoRepository;
    private final ConductorRepository conductorRepository;
    private final BitacoraEnvioRepository bitacoraRepository;
    private final UsuarioRepository usuarioRepository;

    /** Inyeccion de dependencias por constructor (sin @Autowired en campos). */
    public EnvioService(EnvioRepository envioRepository,
                        VehiculoRepository vehiculoRepository,
                        ConductorRepository conductorRepository,
                        BitacoraEnvioRepository bitacoraRepository,
                        UsuarioRepository usuarioRepository) {
        this.envioRepository = envioRepository;
        this.vehiculoRepository = vehiculoRepository;
        this.conductorRepository = conductorRepository;
        this.bitacoraRepository = bitacoraRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /** GET /api/envios/optimizados -> una sola consulta con JOIN FETCH. */
    public List<EnvioResponseDTO> listarOptimizados() {
        return envioRepository.findAllOptimizado().stream()
                .map(EnvioResponseDTO::desde)
                .toList();
    }

    public List<EnvioResponseDTO> listarPorEstado(String estado) {
        validarEstado(estado);
        return envioRepository.findByEstadoOptimizado(estado).stream()
                .map(EnvioResponseDTO::desde)
                .toList();
    }

    public EnvioResponseDTO buscarPorId(Integer id) {
        return EnvioResponseDTO.desde(obtenerOptimizadoOFallar(id));
    }

    public ResumenEnviosDTO resumen() {
        return new ResumenEnviosDTO(
                envioRepository.count(),
                envioRepository.countByEstadoEnvio(Envio.PENDIENTE),
                envioRepository.countByEstadoEnvio(Envio.EN_TRANSITO),
                envioRepository.countByEstadoEnvio(Envio.ENTREGADO),
                envioRepository.countByEstadoEnvio(Envio.CANCELADO),
                vehiculoRepository.countByEstadoNot(Vehiculo.MANTENIMIENTO));
    }

    /** GET /api/envios/{id}/bitacora -> historial de auditoria del envio. */
    public List<BitacoraResponseDTO> listarBitacora(Integer envioId) {
        if (!envioRepository.existsById(envioId)) {
            throw new ResourceNotFoundException("No existe el envio con id " + envioId);
        }
        return bitacoraRepository.findByEnvioOptimizado(envioId).stream()
                .map(BitacoraResponseDTO::desde)
                .toList();
    }

    /** Registra un envio express validando integridad referencial y capacidad. */
    @Transactional
    public EnvioResponseDTO registrar(EnvioRequestDTO solicitud) {
        String codigo = solicitud.getCodigoRastreo().trim().toUpperCase();

        if (envioRepository.existsByCodigoRastreo(codigo)) {
            throw new ReglaNegocioException(
                    "Ya existe un envio registrado con el codigo de rastreo " + codigo);
        }

        Vehiculo vehiculo = vehiculoRepository.findById(solicitud.getVehiculoId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe el vehiculo con id " + solicitud.getVehiculoId()));

        Conductor conductor = conductorRepository.findById(solicitud.getConductorId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe el conductor con id " + solicitud.getConductorId()));

        validarDisponibilidadVehiculo(vehiculo);
        validarCapacidad(vehiculo, solicitud.getPesoKg());

        Envio envio = new Envio();
        envio.setCodigoRastreo(codigo);
        envio.setDireccionDestino(solicitud.getDireccionDestino().trim());
        envio.setPesoKg(solicitud.getPesoKg());
        envio.setCosto(solicitud.getCosto());
        envio.setEstadoEnvio(Envio.PENDIENTE); // todo envio nace PENDIENTE
        envio.setVehiculo(vehiculo);
        envio.setConductor(conductor);

        Envio guardado = envioRepository.save(envio);

        // Primera entrada de la bitacora: deja trazado quien dio de alta el envio.
        bitacoraRepository.save(new BitacoraEnvio(guardado, Envio.PENDIENTE, Envio.PENDIENTE,
                usuarioAutenticado(), "Registro inicial del envio"));

        // Se recarga con JOIN FETCH para devolver el grafo completo al frontend.
        return EnvioResponseDTO.desde(obtenerOptimizadoOFallar(guardado.getId()));
    }

    @Transactional
    public EnvioResponseDTO cambiarEstado(Integer id, CambioEstadoDTO cambio) {
        String nuevoEstado = cambio.getNuevoEstado() == null
                ? null
                : cambio.getNuevoEstado().trim().toUpperCase();
        validarEstado(nuevoEstado);

        Envio envio = envioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el envio con id " + id));

        String estadoAnterior = envio.getEstadoEnvio();
        validarTransicion(envio.getCodigoRastreo(), estadoAnterior, nuevoEstado);

        envio.setEstadoEnvio(nuevoEstado); // el dirty checking emite el UPDATE en el commit

        bitacoraRepository.save(new BitacoraEnvio(envio, estadoAnterior, nuevoEstado,
                usuarioAutenticado(), cambio.getObservaciones()));

        envioRepository.flush();
        return EnvioResponseDTO.desde(obtenerOptimizadoOFallar(id));
    }

    /** Actualizacion masiva con un solo UPDATE, util cuando la unidad entra a mantenimiento. */
    @Transactional
    public int actualizarEstadoMasivoPorVehiculo(Integer vehiculoId, String nuevoEstado,
                                                 String observaciones) {
        String estado = nuevoEstado == null ? null : nuevoEstado.trim().toUpperCase();
        validarEstado(estado);

        if (!vehiculoRepository.existsById(vehiculoId)) {
            throw new ResourceNotFoundException("No existe el vehiculo con id " + vehiculoId);
        }

        Usuario actuante = usuarioAutenticado();
        List<Envio> afectados = envioRepository.findByVehiculoId(vehiculoId);

        for (Envio envio : afectados) {
            if (!envio.getEstadoEnvio().equals(estado)) {
                bitacoraRepository.save(new BitacoraEnvio(envio, envio.getEstadoEnvio(), estado,
                        actuante, observaciones == null
                        ? "Actualizacion masiva por vehiculo " + vehiculoId
                        : observaciones));
            }
        }

        return envioRepository.actualizarEstadoMasivoPorVehiculo(vehiculoId, estado);
    }

    /** Lab 7: flete segun peso y distancia; sin acceso a base de datos para poder parametrizarlo. */
    public double calcularTarifa(double pesoKg, double distanciaKm) {
        if (pesoKg <= 0) {
            throw new ReglaNegocioException("El peso del envio debe ser mayor a cero");
        }
        if (distanciaKm <= 0) {
            throw new ReglaNegocioException("La distancia del envio debe ser mayor a cero");
        }

        double tarifaPorKg = distanciaKm <= DISTANCIA_LOCAL_KM
                ? TARIFA_LOCAL_POR_KG
                : TARIFA_NACIONAL_POR_KG;

        return pesoKg * tarifaPorKg;
    }

    // Validaciones

    private void validarEstado(String estado) {
        if (estado == null || !ESTADOS_VALIDOS.contains(estado)) {
            throw new ReglaNegocioException("Estado de envio invalido: " + estado
                    + ". Valores permitidos: " + ESTADOS_VALIDOS);
        }
    }

    private void validarTransicion(String codigoRastreo, String estadoActual, String nuevoEstado) {
        if (estadoActual.equals(nuevoEstado)) {
            throw new InvalidStateTransitionException(codigoRastreo,
                    "el envio ya se encuentra en estado " + estadoActual);
        }

        if (ESTADOS_FINALES.contains(estadoActual)) {
            throw new InvalidStateTransitionException(codigoRastreo,
                    estadoActual + " es un estado final");
        }

        if (!TRANSICIONES_PERMITIDAS.getOrDefault(estadoActual, Set.of()).contains(nuevoEstado)) {
            throw new InvalidStateTransitionException(codigoRastreo,
                    "de " + estadoActual + " solo se puede pasar a "
                            + TRANSICIONES_PERMITIDAS.getOrDefault(estadoActual, Set.of()));
        }
    }

    private void validarDisponibilidadVehiculo(Vehiculo vehiculo) {
        if (Vehiculo.MANTENIMIENTO.equals(vehiculo.getEstado())) {
            throw new ReglaNegocioException("El vehiculo " + vehiculo.getPlaca()
                    + " esta en MANTENIMIENTO y no puede recibir envios");
        }
    }

    /** Regla central: el peso no puede superar la capacidad, ni solo ni con la carga ya comprometida. */
    private void validarCapacidad(Vehiculo vehiculo, BigDecimal pesoNuevo) {
        BigDecimal capacidad = vehiculo.getCapacidadKg();

        if (pesoNuevo.compareTo(capacidad) > 0) {
            throw new ReglaNegocioException(String.format(
                    "El peso del envio (%.2f kg) supera la capacidad del vehiculo %s (%.2f kg)",
                    pesoNuevo, vehiculo.getPlaca(), capacidad));
        }

        BigDecimal cargaActual = envioRepository.sumarPesoActivoPorVehiculo(vehiculo.getId());
        BigDecimal cargaTotal = cargaActual.add(pesoNuevo);

        if (cargaTotal.compareTo(capacidad) > 0) {
            throw new ReglaNegocioException(String.format(
                    "El vehiculo %s ya transporta %.2f kg; con este envio (%.2f kg) llegaria a "
                            + "%.2f kg y su capacidad maxima es %.2f kg",
                    vehiculo.getPlaca(), cargaActual, pesoNuevo, cargaTotal, capacidad));
        }
    }

    /** Usuario que porta el token JWT de la peticion en curso (Pista 2 del enunciado). */
    private Usuario usuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new ResourceNotFoundException("No hay un usuario autenticado en el contexto");
        }

        String username = auth.getName();
        return usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private Envio obtenerOptimizadoOFallar(Integer id) {
        return envioRepository.findByIdOptimizado(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el envio con id " + id));
    }
}
