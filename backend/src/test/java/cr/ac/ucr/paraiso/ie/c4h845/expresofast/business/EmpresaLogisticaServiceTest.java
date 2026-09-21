package cr.ac.ucr.paraiso.ie.c4h845.expresofast.business;

import cr.ac.ucr.paraiso.ie.c4h845.expresofast.data.EmpresaLogisticaRepository;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.domain.EmpresaLogistica;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.domain.Vehiculo;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.dto.EmpresaDTO;
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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pruebas unitarias del servicio de empresas logisticas con dependencias simuladas. */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmpresaLogisticaService - registro y consulta de empresas")
class EmpresaLogisticaServiceTest {

    @Mock
    private EmpresaLogisticaRepository empresaRepository;

    @InjectMocks
    private EmpresaLogisticaService empresaService;

    @Captor
    private ArgumentCaptor<EmpresaLogistica> empresaCaptor;

    @Test
    @DisplayName("listar devuelve el catalogo completo mapeado a DTO")
    void listar_ConEmpresas_RetornaListaDTO() {
        when(empresaRepository.findAll()).thenReturn(List.of(empresa()));

        List<EmpresaDTO> resultado = empresaService.listar();

        assertEquals(1, resultado.size());
        assertEquals("Transportes Paraiso", resultado.get(0).nombre());
    }

    @Test
    @DisplayName("buscarPorId devuelve la empresa solicitada")
    void buscarPorId_EmpresaExistente_RetornaDTO() {
        when(empresaRepository.findById(1)).thenReturn(Optional.of(empresa()));

        EmpresaDTO resultado = empresaService.buscarPorId(1);

        assertEquals("3-101-778899", resultado.cedulaJuridica());
    }

    @Test
    @DisplayName("buscarPorId con id inexistente lanza ResourceNotFoundException")
    void buscarPorId_EmpresaInexistente_LanzaExcepcion() {
        when(empresaRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> empresaService.buscarPorId(99));
    }

    @Test
    @DisplayName("buscarPorCedulaJuridica devuelve la empresa correspondiente")
    void buscarPorCedulaJuridica_EmpresaExistente_RetornaDTO() {
        when(empresaRepository.findByCedulaJuridica("3-101-778899")).thenReturn(Optional.of(empresa()));

        EmpresaDTO resultado = empresaService.buscarPorCedulaJuridica(" 3-101-778899 ");

        assertEquals("Transportes Paraiso", resultado.nombre());
    }

    @Test
    @DisplayName("buscarPorCedulaJuridica sin coincidencias lanza ResourceNotFoundException")
    void buscarPorCedulaJuridica_EmpresaInexistente_LanzaExcepcion() {
        when(empresaRepository.findByCedulaJuridica("3-101-000000")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> empresaService.buscarPorCedulaJuridica("3-101-000000"));
    }

    @Test
    @DisplayName("registrar con datos validos normaliza el nombre y guarda la empresa")
    void registrar_DatosValidos_RetornaEmpresaDTO() {
        EmpresaLogistica solicitud = empresa();
        solicitud.setNombre("  Transportes Paraiso  ");

        when(empresaRepository.existsByNombre("Transportes Paraiso")).thenReturn(false);
        when(empresaRepository.findByCedulaJuridica("3-101-778899")).thenReturn(Optional.empty());
        when(empresaRepository.save(any(EmpresaLogistica.class))).thenReturn(empresa());

        EmpresaDTO resultado = empresaService.registrar(solicitud);

        assertEquals("Transportes Paraiso", resultado.nombre());
        verify(empresaRepository).save(empresaCaptor.capture());
        assertEquals("Transportes Paraiso", empresaCaptor.getValue().getNombre());
    }

    @Test
    @DisplayName("registrar con un nombre repetido lanza ReglaNegocioException")
    void registrar_NombreDuplicado_LanzaExcepcion() {
        when(empresaRepository.existsByNombre("Transportes Paraiso")).thenReturn(true);

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> empresaService.registrar(empresa()));

        assertTrue(error.getMessage().contains("Ya existe una empresa registrada"));
        verify(empresaRepository, never()).save(any());
    }

    @Test
    @DisplayName("registrar con una cedula juridica repetida lanza ReglaNegocioException")
    void registrar_CedulaDuplicada_LanzaExcepcion() {
        when(empresaRepository.existsByNombre("Transportes Paraiso")).thenReturn(false);
        when(empresaRepository.findByCedulaJuridica("3-101-778899")).thenReturn(Optional.of(empresa()));

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> empresaService.registrar(empresa()));

        assertTrue(error.getMessage().contains("cedula juridica"));
        verify(empresaRepository, never()).save(any());
    }

    @Test
    @DisplayName("eliminar retira una empresa que no tiene vehiculos")
    void eliminar_EmpresaSinVehiculos_EliminaEmpresa() {
        EmpresaLogistica existente = empresa();
        when(empresaRepository.findById(1)).thenReturn(Optional.of(existente));

        empresaService.eliminar(1);

        verify(empresaRepository).delete(existente);
    }

    @Test
    @DisplayName("eliminar una empresa con vehiculos asociados lanza ReglaNegocioException")
    void eliminar_EmpresaConVehiculos_LanzaExcepcion() {
        EmpresaLogistica existente = empresa();
        existente.setVehiculos(List.of(new Vehiculo()));
        when(empresaRepository.findById(1)).thenReturn(Optional.of(existente));

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> empresaService.eliminar(1));

        assertTrue(error.getMessage().contains("tiene vehiculos asociados"));
        verify(empresaRepository, never()).delete(any());
    }

    // Fabrica de datos de prueba

    private EmpresaLogistica empresa() {
        EmpresaLogistica empresa = new EmpresaLogistica();
        empresa.setId(1);
        empresa.setNombre("Transportes Paraiso");
        empresa.setCedulaJuridica("3-101-778899");
        empresa.setTelefono("2574-1010");
        return empresa;
    }
}
