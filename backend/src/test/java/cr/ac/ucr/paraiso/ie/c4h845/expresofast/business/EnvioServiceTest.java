package cr.ac.ucr.paraiso.ie.c4h845.expresofast.business;

import cr.ac.ucr.paraiso.ie.c4h845.expresofast.data.BitacoraEnvioRepository;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.data.ConductorRepository;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.data.EnvioRepository;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.data.UsuarioRepository;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.data.VehiculoRepository;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.domain.BitacoraEnvio;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.domain.Conductor;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.domain.EmpresaLogistica;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pruebas unitarias aisladas del servicio de envios: los repositorios son mocks de Mockito. */
@ExtendWith(MockitoExtension.class)
@DisplayName("EnvioService - reglas de negocio de envios express")
class EnvioServiceTest {

    private static final String USUARIO_AUTENTICADO = "admin";

    @Mock
    private EnvioRepository envioRepository;

    @Mock
    private VehiculoRepository vehiculoRepository;

    @Mock
    private ConductorRepository conductorRepository;

    @Mock
    private BitacoraEnvioRepository bitacoraRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private EnvioService envioService;

    @Captor
    private ArgumentCaptor<Envio> envioCaptor;

    @Captor
    private ArgumentCaptor<BitacoraEnvio> bitacoraCaptor;

    @BeforeEach
    void prepararContextoDeSeguridad() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(USUARIO_AUTENTICADO, "n/a", List.of()));
    }

    @AfterEach
    void limpiarContextoDeSeguridad() {
        SecurityContextHolder.clearContext();
    }

    // Consultas

    @Test
    @DisplayName("listarOptimizados devuelve la lista mapeada a DTO")
    void listarOptimizados_ConEnvios_RetornaListaDTO() {
        when(envioRepository.findAllOptimizado()).thenReturn(List.of(envio(1, Envio.PENDIENTE)));

        List<EnvioResponseDTO> resultado = envioService.listarOptimizados();

        assertEquals(1, resultado.size());
        assertEquals("EXP-1001", resultado.get(0).codigoRastreo());
        assertEquals("CRC-1001", resultado.get(0).placaVehiculo());
        verify(envioRepository).findAllOptimizado();
    }

    @Test
    @DisplayName("listarPorEstado filtra por un estado valido")
    void listarPorEstado_EstadoValido_RetornaFiltrado() {
        when(envioRepository.findByEstadoOptimizado(Envio.EN_TRANSITO))
                .thenReturn(List.of(envio(2, Envio.EN_TRANSITO)));

        List<EnvioResponseDTO> resultado = envioService.listarPorEstado(Envio.EN_TRANSITO);

        assertEquals(1, resultado.size());
        assertEquals(Envio.EN_TRANSITO, resultado.get(0).estadoEnvio());
    }

    @Test
    @DisplayName("listarPorEstado con un estado inexistente lanza ReglaNegocioException")
    void listarPorEstado_EstadoInvalido_LanzaExcepcion() {
        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> envioService.listarPorEstado("EXTRAVIADO"));

        assertTrue(error.getMessage().contains("Estado de envio invalido"));
        verify(envioRepository, never()).findByEstadoOptimizado(any());
    }

    @Test
    @DisplayName("buscarPorId devuelve el envio cargado con JOIN FETCH")
    void buscarPorId_EnvioExistente_RetornaDTO() {
        when(envioRepository.findByIdOptimizado(1)).thenReturn(Optional.of(envio(1, Envio.PENDIENTE)));

        EnvioResponseDTO resultado = envioService.buscarPorId(1);

        assertEquals("EXP-1001", resultado.codigoRastreo());
        assertEquals("Transportes Paraiso", resultado.empresaNombre());
    }

    @Test
    @DisplayName("buscarPorId con id inexistente lanza ResourceNotFoundException")
    void buscarPorId_EnvioInexistente_LanzaExcepcion() {
        when(envioRepository.findByIdOptimizado(99)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> envioService.buscarPorId(99));
    }

    @Test
    @DisplayName("resumen consolida los contadores por estado")
    void resumen_ConEnvios_RetornaContadores() {
        when(envioRepository.count()).thenReturn(10L);
        when(envioRepository.countByEstadoEnvio(Envio.PENDIENTE)).thenReturn(4L);
        when(envioRepository.countByEstadoEnvio(Envio.EN_TRANSITO)).thenReturn(3L);
        when(envioRepository.countByEstadoEnvio(Envio.ENTREGADO)).thenReturn(2L);
        when(envioRepository.countByEstadoEnvio(Envio.CANCELADO)).thenReturn(1L);

        ResumenEnviosDTO resumen = envioService.resumen();

        assertEquals(10L, resumen.total());
        assertEquals(4L, resumen.pendientes());
        assertEquals(1L, resumen.cancelados());
    }

    @Test
    @DisplayName("listarBitacora devuelve el historial del envio")
    void listarBitacora_EnvioExistente_RetornaHistorial() {
        Envio envio = envio(1, Envio.EN_TRANSITO);
        when(envioRepository.existsById(1)).thenReturn(true);
        when(bitacoraRepository.findByEnvioOptimizado(1)).thenReturn(List.of(
                new BitacoraEnvio(envio, Envio.PENDIENTE, Envio.EN_TRANSITO, usuario(), "Salida a ruta")));

        List<BitacoraResponseDTO> historial = envioService.listarBitacora(1);

        assertEquals(1, historial.size());
        assertEquals(Envio.EN_TRANSITO, historial.get(0).estadoNuevo());
    }

    @Test
    @DisplayName("listarBitacora de un envio inexistente lanza ResourceNotFoundException")
    void listarBitacora_EnvioInexistente_LanzaExcepcion() {
        when(envioRepository.existsById(99)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> envioService.listarBitacora(99));
        verify(bitacoraRepository, never()).findByEnvioOptimizado(any());
    }

    // Registro de envios

    @Test
    @DisplayName("crearEnvio con datos validos retorna el DTO y nace en estado PENDIENTE")
    void crearEnvio_DatosValidos_RetornaEnvioDTO() {
        EnvioRequestDTO solicitud = solicitud("exp-1001", new BigDecimal("50.00"));

        when(envioRepository.existsByCodigoRastreo("EXP-1001")).thenReturn(false);
        when(vehiculoRepository.findById(1)).thenReturn(Optional.of(vehiculo(Vehiculo.DISPONIBLE)));
        when(conductorRepository.findById(1)).thenReturn(Optional.of(conductor()));
        when(envioRepository.sumarPesoActivoPorVehiculo(1)).thenReturn(BigDecimal.ZERO);
        when(usuarioRepository.findByUsername(USUARIO_AUTENTICADO)).thenReturn(Optional.of(usuario()));
        when(envioRepository.save(any(Envio.class))).thenReturn(envio(1, Envio.PENDIENTE));
        when(envioRepository.findByIdOptimizado(1)).thenReturn(Optional.of(envio(1, Envio.PENDIENTE)));

        EnvioResponseDTO resultado = envioService.registrar(solicitud);

        assertEquals(Envio.PENDIENTE, resultado.estadoEnvio());
        verify(envioRepository).save(envioCaptor.capture());
        assertEquals(Envio.PENDIENTE, envioCaptor.getValue().getEstadoEnvio());
        assertEquals("EXP-1001", envioCaptor.getValue().getCodigoRastreo());

        // La primera entrada de bitacora deja trazado el alta del envio.
        verify(bitacoraRepository).save(bitacoraCaptor.capture());
        assertEquals(Envio.PENDIENTE, bitacoraCaptor.getValue().getEstadoNuevo());
    }

    @Test
    @DisplayName("crearEnvio con un codigo de rastreo repetido lanza ReglaNegocioException")
    void crearEnvio_CodigoDuplicado_LanzaExcepcion() {
        when(envioRepository.existsByCodigoRastreo("EXP-1001")).thenReturn(true);

        assertThrows(ReglaNegocioException.class,
                () -> envioService.registrar(solicitud("EXP-1001", new BigDecimal("10.00"))));
        verify(envioRepository, never()).save(any());
    }

    @Test
    @DisplayName("crearEnvio con vehiculo inexistente lanza ResourceNotFoundException")
    void crearEnvio_VehiculoInexistente_LanzaExcepcion() {
        when(envioRepository.existsByCodigoRastreo("EXP-1001")).thenReturn(false);
        when(vehiculoRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> envioService.registrar(solicitud("EXP-1001", new BigDecimal("10.00"))));
    }

    @Test
    @DisplayName("crearEnvio con conductor inexistente lanza ResourceNotFoundException")
    void crearEnvio_ConductorInexistente_LanzaExcepcion() {
        when(envioRepository.existsByCodigoRastreo("EXP-1001")).thenReturn(false);
        when(vehiculoRepository.findById(1)).thenReturn(Optional.of(vehiculo(Vehiculo.DISPONIBLE)));
        when(conductorRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> envioService.registrar(solicitud("EXP-1001", new BigDecimal("10.00"))));
    }

    @Test
    @DisplayName("crearEnvio sobre un vehiculo en MANTENIMIENTO lanza ReglaNegocioException")
    void crearEnvio_VehiculoEnMantenimiento_LanzaExcepcion() {
        when(envioRepository.existsByCodigoRastreo("EXP-1001")).thenReturn(false);
        when(vehiculoRepository.findById(1)).thenReturn(Optional.of(vehiculo(Vehiculo.MANTENIMIENTO)));
        when(conductorRepository.findById(1)).thenReturn(Optional.of(conductor()));

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> envioService.registrar(solicitud("EXP-1001", new BigDecimal("10.00"))));

        assertTrue(error.getMessage().contains("MANTENIMIENTO"));
        verify(envioRepository, never()).save(any());
    }

    @Test
    @DisplayName("crearEnvio con un paquete mas pesado que la capacidad lanza ReglaNegocioException")
    void crearEnvio_VehiculoSinCapacidad_LanzaExcepcion() {
        when(envioRepository.existsByCodigoRastreo("EXP-1001")).thenReturn(false);
        when(vehiculoRepository.findById(1)).thenReturn(Optional.of(vehiculo(Vehiculo.DISPONIBLE)));
        when(conductorRepository.findById(1)).thenReturn(Optional.of(conductor()));

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> envioService.registrar(solicitud("EXP-1001", new BigDecimal("1500.00"))));

        assertTrue(error.getMessage().contains("supera la capacidad"));
        verify(envioRepository, never()).save(any());
    }

    @Test
    @DisplayName("crearEnvio que excede la carga ya comprometida lanza ReglaNegocioException")
    void crearEnvio_CargaAcumuladaExcedida_LanzaExcepcion() {
        when(envioRepository.existsByCodigoRastreo("EXP-1001")).thenReturn(false);
        when(vehiculoRepository.findById(1)).thenReturn(Optional.of(vehiculo(Vehiculo.DISPONIBLE)));
        when(conductorRepository.findById(1)).thenReturn(Optional.of(conductor()));
        when(envioRepository.sumarPesoActivoPorVehiculo(1)).thenReturn(new BigDecimal("950.00"));

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> envioService.registrar(solicitud("EXP-1001", new BigDecimal("100.00"))));

        assertTrue(error.getMessage().contains("ya transporta"));
        verify(envioRepository, never()).save(any());
    }

    @Test
    @DisplayName("crearEnvio sin usuario en el contexto de seguridad lanza ResourceNotFoundException")
    void crearEnvio_SinUsuarioAutenticado_LanzaExcepcion() {
        SecurityContextHolder.clearContext();

        when(envioRepository.existsByCodigoRastreo("EXP-1001")).thenReturn(false);
        when(vehiculoRepository.findById(1)).thenReturn(Optional.of(vehiculo(Vehiculo.DISPONIBLE)));
        when(conductorRepository.findById(1)).thenReturn(Optional.of(conductor()));
        when(envioRepository.sumarPesoActivoPorVehiculo(1)).thenReturn(BigDecimal.ZERO);
        when(envioRepository.save(any(Envio.class))).thenReturn(envio(1, Envio.PENDIENTE));

        assertThrows(ResourceNotFoundException.class,
                () -> envioService.registrar(solicitud("EXP-1001", new BigDecimal("10.00"))));
    }

    @Test
    @DisplayName("crearEnvio con un usuario ausente en la base lanza ResourceNotFoundException")
    void crearEnvio_UsuarioNoRegistrado_LanzaExcepcion() {
        when(envioRepository.existsByCodigoRastreo("EXP-1001")).thenReturn(false);
        when(vehiculoRepository.findById(1)).thenReturn(Optional.of(vehiculo(Vehiculo.DISPONIBLE)));
        when(conductorRepository.findById(1)).thenReturn(Optional.of(conductor()));
        when(envioRepository.sumarPesoActivoPorVehiculo(1)).thenReturn(BigDecimal.ZERO);
        when(envioRepository.save(any(Envio.class))).thenReturn(envio(1, Envio.PENDIENTE));
        when(usuarioRepository.findByUsername(USUARIO_AUTENTICADO)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> envioService.registrar(solicitud("EXP-1001", new BigDecimal("10.00"))));
    }

    // Transiciones de estado

    @Test
    @DisplayName("actualizarEstado con una transicion valida cambia el estado y registra la bitacora")
    void actualizarEstado_TransicionValida_ActualizaEstado() {
        Envio envio = envio(1, Envio.PENDIENTE);
        when(envioRepository.findById(1)).thenReturn(Optional.of(envio));
        when(usuarioRepository.findByUsername(USUARIO_AUTENTICADO)).thenReturn(Optional.of(usuario()));
        when(envioRepository.findByIdOptimizado(1)).thenReturn(Optional.of(envio(1, Envio.EN_TRANSITO)));

        EnvioResponseDTO resultado = envioService.cambiarEstado(1, cambio(Envio.EN_TRANSITO));

        assertEquals(Envio.EN_TRANSITO, resultado.estadoEnvio());
        assertEquals(Envio.EN_TRANSITO, envio.getEstadoEnvio());
        verify(bitacoraRepository).save(bitacoraCaptor.capture());
        assertEquals(Envio.PENDIENTE, bitacoraCaptor.getValue().getEstadoAnterior());
        verify(envioRepository).flush();
    }

    @Test
    @DisplayName("actualizarEstado de ENTREGADO a EN_TRANSITO lanza InvalidStateTransitionException")
    void actualizarEstado_TransicionInvalida_LanzaExcepcion() {
        when(envioRepository.findById(1)).thenReturn(Optional.of(envio(1, Envio.ENTREGADO)));

        InvalidStateTransitionException error = assertThrows(InvalidStateTransitionException.class,
                () -> envioService.cambiarEstado(1, cambio(Envio.EN_TRANSITO)));

        assertTrue(error.getMessage().contains("estado final"));
        verify(bitacoraRepository, never()).save(any());
    }

    @Test
    @DisplayName("actualizarEstado hacia el mismo estado lanza InvalidStateTransitionException")
    void actualizarEstado_MismoEstado_LanzaExcepcion() {
        when(envioRepository.findById(1)).thenReturn(Optional.of(envio(1, Envio.PENDIENTE)));

        assertThrows(InvalidStateTransitionException.class,
                () -> envioService.cambiarEstado(1, cambio(Envio.PENDIENTE)));
    }

    @Test
    @DisplayName("actualizarEstado de PENDIENTE a ENTREGADO omite EN_TRANSITO y lanza excepcion")
    void actualizarEstado_SaltoDeSecuencia_LanzaExcepcion() {
        when(envioRepository.findById(1)).thenReturn(Optional.of(envio(1, Envio.PENDIENTE)));

        InvalidStateTransitionException error = assertThrows(InvalidStateTransitionException.class,
                () -> envioService.cambiarEstado(1, cambio(Envio.ENTREGADO)));

        assertTrue(error.getMessage().contains("solo se puede pasar a"));
    }

    @Test
    @DisplayName("cancelarEnvio sobre un envio ya ENTREGADO lanza InvalidStateTransitionException")
    void cancelarEnvio_EnvioEntregado_LanzaExcepcion() {
        when(envioRepository.findById(1)).thenReturn(Optional.of(envio(1, Envio.ENTREGADO)));

        assertThrows(InvalidStateTransitionException.class,
                () -> envioService.cambiarEstado(1, cambio(Envio.CANCELADO)));
        verify(envioRepository, never()).flush();
    }

    @Test
    @DisplayName("actualizarEstado con estado nulo lanza ReglaNegocioException")
    void actualizarEstado_EstadoNulo_LanzaExcepcion() {
        assertThrows(ReglaNegocioException.class, () -> envioService.cambiarEstado(1, cambio(null)));
        verify(envioRepository, never()).findById(any());
    }

    @Test
    @DisplayName("actualizarEstado de un envio inexistente lanza ResourceNotFoundException")
    void actualizarEstado_EnvioInexistente_LanzaExcepcion() {
        when(envioRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> envioService.cambiarEstado(99, cambio(Envio.EN_TRANSITO)));
    }

    // Actualizacion masiva por vehiculo

    @Test
    @DisplayName("actualizarEstadoMasivo cambia los envios del vehiculo y audita solo los afectados")
    void actualizarEstadoMasivo_VehiculoExistente_ActualizaEnvios() {
        when(vehiculoRepository.existsById(1)).thenReturn(true);
        when(usuarioRepository.findByUsername(USUARIO_AUTENTICADO)).thenReturn(Optional.of(usuario()));
        when(envioRepository.findByVehiculoId(1)).thenReturn(List.of(
                envio(1, Envio.PENDIENTE), envio(2, Envio.CANCELADO)));
        when(envioRepository.actualizarEstadoMasivoPorVehiculo(1, Envio.CANCELADO)).thenReturn(1);

        int afectados = envioService.actualizarEstadoMasivoPorVehiculo(1, "cancelado", null);

        assertEquals(1, afectados);
        // El envio que ya estaba CANCELADO no genera entrada de bitacora.
        verify(bitacoraRepository).save(any(BitacoraEnvio.class));
    }

    @Test
    @DisplayName("actualizarEstadoMasivo sobre un vehiculo inexistente lanza ResourceNotFoundException")
    void actualizarEstadoMasivo_VehiculoInexistente_LanzaExcepcion() {
        when(vehiculoRepository.existsById(99)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> envioService.actualizarEstadoMasivoPorVehiculo(99, Envio.CANCELADO, "baja"));
        verify(envioRepository, never()).actualizarEstadoMasivoPorVehiculo(any(), any());
    }

    // Tarifas (pruebas parametrizadas)

    @ParameterizedTest
    @CsvSource({
            "5.0, 10.0, 2500.0",
            "15.0, 50.0, 7500.0",
            "100.0, 2.5, 12000.0",
            "1.0, 5.0, 120.0",
            "2.5, 120.0, 1250.0"
    })
    @DisplayName("Debe calcular la tarifa correcta segun peso y distancia")
    void calcularTarifa_CasosVariados_CalculaCorrectamente(
            double pesoKg, double distanciaKm, double tarifaEsperada) {

        double tarifaCalculada = envioService.calcularTarifa(pesoKg, distanciaKm);

        assertEquals(tarifaEsperada, tarifaCalculada, 0.01);
    }

    @ParameterizedTest
    @CsvSource({
            "0.0, 10.0",
            "-5.0, 10.0",
            "10.0, 0.0",
            "10.0, -1.0"
    })
    @DisplayName("calcularTarifa con peso o distancia no positivos lanza ReglaNegocioException")
    void calcularTarifa_ParametrosInvalidos_LanzaExcepcion(double pesoKg, double distanciaKm) {
        assertThrows(ReglaNegocioException.class,
                () -> envioService.calcularTarifa(pesoKg, distanciaKm));
    }

    // Fabricas de datos de prueba

    private EnvioRequestDTO solicitud(String codigoRastreo, BigDecimal pesoKg) {
        EnvioRequestDTO solicitud = new EnvioRequestDTO();
        solicitud.setCodigoRastreo(codigoRastreo);
        solicitud.setDireccionDestino(" Paraiso, Cartago ");
        solicitud.setPesoKg(pesoKg);
        solicitud.setCosto(new BigDecimal("7500.00"));
        solicitud.setVehiculoId(1);
        solicitud.setConductorId(1);
        return solicitud;
    }

    private CambioEstadoDTO cambio(String nuevoEstado) {
        CambioEstadoDTO cambio = new CambioEstadoDTO();
        cambio.setNuevoEstado(nuevoEstado);
        cambio.setObservaciones("Cambio ejecutado por la suite de pruebas");
        return cambio;
    }

    private Envio envio(Integer id, String estado) {
        Envio envio = new Envio();
        envio.setId(id);
        envio.setCodigoRastreo("EXP-100" + id);
        envio.setDireccionDestino("Paraiso, Cartago");
        envio.setPesoKg(new BigDecimal("50.00"));
        envio.setCosto(new BigDecimal("7500.00"));
        envio.setEstadoEnvio(estado);
        envio.setVehiculo(vehiculo(Vehiculo.DISPONIBLE));
        envio.setConductor(conductor());
        return envio;
    }

    private Vehiculo vehiculo(String estado) {
        EmpresaLogistica empresa = new EmpresaLogistica();
        empresa.setId(1);
        empresa.setNombre("Transportes Paraiso");

        Vehiculo vehiculo = new Vehiculo();
        vehiculo.setId(1);
        vehiculo.setPlaca("CRC-1001");
        vehiculo.setCapacidadKg(new BigDecimal("1000.00"));
        vehiculo.setEstado(estado);
        vehiculo.setEmpresa(empresa);
        return vehiculo;
    }

    private Conductor conductor() {
        Conductor conductor = new Conductor();
        conductor.setId(1);
        conductor.setNombre("Ana");
        conductor.setApellidos("Morales Vargas");
        conductor.setLicencia("B1-334455");
        conductor.setTelefono("8888-1122");
        return conductor;
    }

    private Usuario usuario() {
        Usuario usuario = new Usuario();
        usuario.setId(1);
        usuario.setUsername(USUARIO_AUTENTICADO);
        usuario.setNombreCompleto("Administrador ExpresoFast");
        return usuario;
    }
}
