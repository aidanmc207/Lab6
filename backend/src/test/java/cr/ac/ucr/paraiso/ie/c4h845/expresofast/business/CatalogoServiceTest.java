package cr.ac.ucr.paraiso.ie.c4h845.expresofast.business;

import cr.ac.ucr.paraiso.ie.c4h845.expresofast.data.ConductorRepository;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.data.EmpresaLogisticaRepository;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.data.VehiculoRepository;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.domain.Conductor;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.domain.EmpresaLogistica;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.domain.Vehiculo;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.dto.ConductorDTO;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.dto.EmpresaDTO;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.dto.VehiculoDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pruebas unitarias de los catalogos que alimentan los desplegables del tablero. */
@ExtendWith(MockitoExtension.class)
@DisplayName("CatalogoService - catalogos de apoyo del formulario")
class CatalogoServiceTest {

    @Mock
    private VehiculoRepository vehiculoRepository;

    @Mock
    private ConductorRepository conductorRepository;

    @Mock
    private EmpresaLogisticaRepository empresaRepository;

    @InjectMocks
    private CatalogoService catalogoService;

    @Test
    @DisplayName("listarVehiculos usa la consulta con JOIN FETCH de la empresa")
    void listarVehiculos_ConVehiculos_RetornaListaDTO() {
        when(vehiculoRepository.findAllConEmpresa()).thenReturn(List.of(vehiculo()));

        List<VehiculoDTO> resultado = catalogoService.listarVehiculos();

        assertEquals(1, resultado.size());
        assertEquals("CRC-1001", resultado.get(0).placa());
        assertEquals("Transportes Paraiso", resultado.get(0).empresaNombre());
        verify(vehiculoRepository).findAllConEmpresa();
    }

    @Test
    @DisplayName("listarConductores devuelve el catalogo de conductores")
    void listarConductores_ConConductores_RetornaListaDTO() {
        when(conductorRepository.findAll()).thenReturn(List.of(conductor()));

        List<ConductorDTO> resultado = catalogoService.listarConductores();

        assertEquals(1, resultado.size());
        assertEquals("B1-334455", resultado.get(0).licencia());
    }

    @Test
    @DisplayName("listarEmpresas devuelve el catalogo de empresas")
    void listarEmpresas_ConEmpresas_RetornaListaDTO() {
        when(empresaRepository.findAll()).thenReturn(List.of(empresa()));

        List<EmpresaDTO> resultado = catalogoService.listarEmpresas();

        assertEquals(1, resultado.size());
        assertEquals("Transportes Paraiso", resultado.get(0).nombre());
    }

    @Test
    @DisplayName("listarEmpresas sin registros devuelve una lista vacia")
    void listarEmpresas_SinEmpresas_RetornaListaVacia() {
        when(empresaRepository.findAll()).thenReturn(List.of());

        assertTrue(catalogoService.listarEmpresas().isEmpty());
    }

    // Fabricas de datos de prueba

    private Vehiculo vehiculo() {
        Vehiculo vehiculo = new Vehiculo();
        vehiculo.setId(1);
        vehiculo.setPlaca("CRC-1001");
        vehiculo.setCapacidadKg(new BigDecimal("1000.00"));
        vehiculo.setEstado(Vehiculo.DISPONIBLE);
        vehiculo.setEmpresa(empresa());
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

    private EmpresaLogistica empresa() {
        EmpresaLogistica empresa = new EmpresaLogistica();
        empresa.setId(1);
        empresa.setNombre("Transportes Paraiso");
        empresa.setCedulaJuridica("3-101-778899");
        empresa.setTelefono("2574-1010");
        return empresa;
    }
}
