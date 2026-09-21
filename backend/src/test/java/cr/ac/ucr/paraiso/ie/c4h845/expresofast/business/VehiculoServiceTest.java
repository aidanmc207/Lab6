package cr.ac.ucr.paraiso.ie.c4h845.expresofast.business;

import cr.ac.ucr.paraiso.ie.c4h845.expresofast.data.EmpresaLogisticaRepository;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.data.EnvioRepository;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.data.VehiculoRepository;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.domain.EmpresaLogistica;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.domain.Envio;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.domain.Vehiculo;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.dto.VehiculoDTO;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.dto.VehiculoRequestDTO;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.exception.ReglaNegocioException;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

/** Pruebas unitarias del servicio de flota, aisladas de la base de datos con Mockito. */
@ExtendWith(MockitoExtension.class)
@DisplayName("VehiculoService - gestion de la flota")
class VehiculoServiceTest {

    @Mock
    private VehiculoRepository vehiculoRepository;

    @Mock
    private EmpresaLogisticaRepository empresaRepository;

    @Mock
    private EnvioRepository envioRepository;

    @InjectMocks
    private VehiculoService vehiculoService;

    @Captor
    private ArgumentCaptor<Vehiculo> vehiculoCaptor;

    @Test
    @DisplayName("listar devuelve la flota con su empresa asociada")
    void listar_ConVehiculos_RetornaListaDTO() {
        when(vehiculoRepository.findAllConEmpresa()).thenReturn(List.of(vehiculo(Vehiculo.DISPONIBLE)));

        List<VehiculoDTO> resultado = vehiculoService.listar();

        assertEquals(1, resultado.size());
        assertEquals("CRC-1001", resultado.get(0).placa());
        assertEquals("Transportes Paraiso", resultado.get(0).empresaNombre());
    }

    @Test
    @DisplayName("buscarPorId devuelve el vehiculo solicitado")
    void buscarPorId_VehiculoExistente_RetornaDTO() {
        when(vehiculoRepository.findById(1)).thenReturn(Optional.of(vehiculo(Vehiculo.DISPONIBLE)));

        VehiculoDTO resultado = vehiculoService.buscarPorId(1);

        assertEquals("CRC-1001", resultado.placa());
    }

    @Test
    @DisplayName("buscarPorId con id inexistente lanza ResourceNotFoundException")
    void buscarPorId_VehiculoInexistente_LanzaExcepcion() {
        when(vehiculoRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> vehiculoService.buscarPorId(99));
    }

    @Test
    @DisplayName("registrarVehiculo con datos validos normaliza la placa y guarda")
    void registrarVehiculo_DatosValidos_RetornaVehiculoDTO() {
        when(vehiculoRepository.findByPlaca("CRC-2002")).thenReturn(Optional.empty());
        when(empresaRepository.findById(1)).thenReturn(Optional.of(empresa()));
        when(vehiculoRepository.save(any(Vehiculo.class))).thenReturn(vehiculo(Vehiculo.DISPONIBLE));

        VehiculoDTO resultado = vehiculoService.registrar(solicitud(" crc-2002 ", Vehiculo.DISPONIBLE));

        assertEquals("CRC-1001", resultado.placa());
        verify(vehiculoRepository).save(vehiculoCaptor.capture());
        assertEquals("CRC-2002", vehiculoCaptor.getValue().getPlaca());
        assertEquals(1, vehiculoCaptor.getValue().getEmpresa().getId());
    }

    @Test
    @DisplayName("registrarVehiculo con placa duplicada lanza ReglaNegocioException")
    void registrarVehiculo_PlacaDuplicada_LanzaExcepcion() {
        when(vehiculoRepository.findByPlaca("CRC-1001"))
                .thenReturn(Optional.of(vehiculo(Vehiculo.DISPONIBLE)));

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> vehiculoService.registrar(solicitud("CRC-1001", Vehiculo.DISPONIBLE)));

        assertTrue(error.getMessage().contains("Ya existe un vehiculo con la placa"));
        verify(vehiculoRepository, never()).save(any());
    }

    @Test
    @DisplayName("registrarVehiculo con empresa inexistente lanza ResourceNotFoundException")
    void registrarVehiculo_EmpresaInexistente_LanzaExcepcion() {
        when(vehiculoRepository.findByPlaca("CRC-2002")).thenReturn(Optional.empty());
        when(empresaRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> vehiculoService.registrar(solicitud("CRC-2002", Vehiculo.DISPONIBLE)));
        verify(vehiculoRepository, never()).save(any());
    }

    @Test
    @DisplayName("actualizar cambia los datos del vehiculo por dirty checking")
    void actualizar_DatosValidos_RetornaVehiculoDTO() {
        Vehiculo existente = vehiculo(Vehiculo.DISPONIBLE);
        when(vehiculoRepository.findById(1)).thenReturn(Optional.of(existente));
        when(vehiculoRepository.findByPlaca("CRC-1001")).thenReturn(Optional.of(existente));
        when(empresaRepository.findById(1)).thenReturn(Optional.of(empresa()));

        VehiculoDTO resultado = vehiculoService.actualizar(1,
                solicitud("CRC-1001", Vehiculo.MANTENIMIENTO));

        assertEquals(Vehiculo.MANTENIMIENTO, resultado.estado());
        assertEquals(Vehiculo.MANTENIMIENTO, existente.getEstado());
    }

    @Test
    @DisplayName("actualizar con la placa de otro vehiculo lanza ReglaNegocioException")
    void actualizar_PlacaDeOtroVehiculo_LanzaExcepcion() {
        Vehiculo otro = vehiculo(Vehiculo.DISPONIBLE);
        otro.setId(2);

        when(vehiculoRepository.findById(1)).thenReturn(Optional.of(vehiculo(Vehiculo.DISPONIBLE)));
        when(vehiculoRepository.findByPlaca("CRC-1001")).thenReturn(Optional.of(otro));

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> vehiculoService.actualizar(1, solicitud("CRC-1001", Vehiculo.DISPONIBLE)));

        assertTrue(error.getMessage().contains("Ya existe otro vehiculo"));
    }

    @Test
    @DisplayName("eliminar retira de la flota un vehiculo sin envios asociados")
    void eliminar_VehiculoSinEnvios_EliminaVehiculo() {
        Vehiculo existente = vehiculo(Vehiculo.DISPONIBLE);
        when(vehiculoRepository.findById(1)).thenReturn(Optional.of(existente));
        when(envioRepository.findByVehiculoId(1)).thenReturn(List.of());

        vehiculoService.eliminar(1);

        verify(vehiculoRepository).delete(existente);
    }

    @Test
    @DisplayName("eliminar un vehiculo con envios asociados lanza ReglaNegocioException")
    void eliminar_VehiculoConEnvios_LanzaExcepcion() {
        when(vehiculoRepository.findById(1)).thenReturn(Optional.of(vehiculo(Vehiculo.DISPONIBLE)));
        when(envioRepository.findByVehiculoId(1)).thenReturn(List.of(new Envio()));

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> vehiculoService.eliminar(1));

        assertTrue(error.getMessage().contains("tiene envios asociados"));
        verify(vehiculoRepository, never()).delete(any());
    }

    @Test
    @DisplayName("eliminar un vehiculo inexistente lanza ResourceNotFoundException")
    void eliminar_VehiculoInexistente_LanzaExcepcion() {
        when(vehiculoRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> vehiculoService.eliminar(99));
    }

    // Fabricas de datos de prueba

    private VehiculoRequestDTO solicitud(String placa, String estado) {
        VehiculoRequestDTO solicitud = new VehiculoRequestDTO();
        solicitud.setPlaca(placa);
        solicitud.setCapacidadKg(new BigDecimal("1000.00"));
        solicitud.setEstado(estado);
        solicitud.setEmpresaId(1);
        return solicitud;
    }

    private Vehiculo vehiculo(String estado) {
        Vehiculo vehiculo = new Vehiculo();
        vehiculo.setId(1);
        vehiculo.setPlaca("CRC-1001");
        vehiculo.setCapacidadKg(new BigDecimal("1000.00"));
        vehiculo.setEstado(estado);
        vehiculo.setEmpresa(empresa());
        return vehiculo;
    }

    private EmpresaLogistica empresa() {
        EmpresaLogistica empresa = new EmpresaLogistica();
        empresa.setId(1);
        empresa.setNombre("Transportes Paraiso");
        empresa.setCedulaJuridica("3-101-778899");
        empresa.setTelefono("2574-1010");
        return empresa;
    }
}
