package cr.ac.ucr.paraiso.ie.c4h845.expresofast.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.business.EnvioService;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.config.SecurityConfig;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.domain.Envio;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.dto.BitacoraResponseDTO;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.dto.EnvioRequestDTO;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.dto.EnvioResponseDTO;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.dto.ResumenEnviosDTO;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.exception.InvalidStateTransitionException;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.exception.ResourceNotFoundException;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Prueba de corte del controlador de envios: se simula el servicio y se apagan los filtros JWT. */
@WebMvcTest(controllers = EnvioController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtAuthenticationFilter.class}))
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("EnvioController - contrato HTTP de /api/envios")
class EnvioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EnvioService envioService;

    @Test
    @DisplayName("GET /api/envios/{id} retorna 200 OK con los atributos del envio")
    void buscarPorId_EnvioExistente_Retorna200() throws Exception {
        when(envioService.buscarPorId(1)).thenReturn(envioDTO(1, Envio.PENDIENTE));

        mockMvc.perform(get("/api/envios/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoRastreo").value("EXP-1001"))
                .andExpect(jsonPath("$.estadoEnvio").value(Envio.PENDIENTE))
                .andExpect(jsonPath("$.placaVehiculo").value("CRC-1001"));
    }

    @Test
    @DisplayName("GET /api/envios/{id} inexistente retorna 404 Not Found")
    void buscarPorId_EnvioInexistente_Retorna404() throws Exception {
        when(envioService.buscarPorId(99))
                .thenThrow(new ResourceNotFoundException("No existe el envio con id 99"));

        mockMvc.perform(get("/api/envios/{id}", 99))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.mensaje").value("No existe el envio con id 99"))
                .andExpect(jsonPath("$.path").value("/api/envios/99"));
    }

    @Test
    @DisplayName("GET /api/envios sin filtro retorna la lista optimizada")
    void listar_SinFiltro_Retorna200() throws Exception {
        when(envioService.listarOptimizados()).thenReturn(List.of(envioDTO(1, Envio.PENDIENTE)));

        mockMvc.perform(get("/api/envios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigoRastreo").value("EXP-1001"));
    }

    @Test
    @DisplayName("GET /api/envios?estado=EN_TRANSITO delega en el filtrado por estado")
    void listar_ConFiltroDeEstado_Retorna200() throws Exception {
        when(envioService.listarPorEstado(Envio.EN_TRANSITO))
                .thenReturn(List.of(envioDTO(2, Envio.EN_TRANSITO)));

        mockMvc.perform(get("/api/envios").param("estado", "en_transito"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estadoEnvio").value(Envio.EN_TRANSITO));
    }

    @Test
    @DisplayName("GET /api/envios/resumen retorna los contadores por estado")
    void resumen_ConEnvios_Retorna200() throws Exception {
        when(envioService.resumen()).thenReturn(new ResumenEnviosDTO(10, 4, 3, 2, 1));

        mockMvc.perform(get("/api/envios/resumen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(10))
                .andExpect(jsonPath("$.pendientes").value(4));
    }

    @Test
    @DisplayName("GET /api/envios/{id}/bitacora retorna el historial de auditoria")
    void bitacora_EnvioExistente_Retorna200() throws Exception {
        when(envioService.listarBitacora(1)).thenReturn(List.of(new BitacoraResponseDTO(
                1, Envio.PENDIENTE, Envio.EN_TRANSITO, LocalDateTime.now(), "admin", "Salida a ruta")));

        mockMvc.perform(get("/api/envios/{id}/bitacora", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estadoNuevo").value(Envio.EN_TRANSITO))
                .andExpect(jsonPath("$[0].usuario").value("admin"));
    }

    @Test
    @DisplayName("POST /api/envios con datos validos retorna 201 Created")
    void registrar_PayloadValido_Retorna201() throws Exception {
        when(envioService.registrar(any(EnvioRequestDTO.class)))
                .thenReturn(envioDTO(1, Envio.PENDIENTE));

        mockMvc.perform(post("/api/envios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(solicitudValida())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigoRastreo").value("EXP-1001"))
                .andExpect(jsonPath("$.estadoEnvio").value(Envio.PENDIENTE));
    }

    @Test
    @DisplayName("POST /api/envios con campos nulos o vacios retorna 400 Bad Request")
    void registrar_PayloadInvalido_Retorna400() throws Exception {
        EnvioRequestDTO invalida = new EnvioRequestDTO();
        invalida.setCodigoRastreo("");
        invalida.setDireccionDestino("");
        invalida.setPesoKg(new BigDecimal("-5.00"));

        mockMvc.perform(post("/api/envios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalida)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errores.codigoRastreo").exists())
                .andExpect(jsonPath("$.errores.direccionDestino").exists())
                .andExpect(jsonPath("$.errores.pesoKg").exists())
                .andExpect(jsonPath("$.errores.vehiculoId").exists())
                .andExpect(jsonPath("$.errores.conductorId").exists());
    }

    @Test
    @DisplayName("POST /api/envios con un JSON malformado retorna 400 Bad Request")
    void registrar_JsonMalformado_Retorna400() throws Exception {
        mockMvc.perform(post("/api/envios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ esto no es json }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("PATCH /api/envios/{id}/estado con una transicion valida retorna 200 OK")
    void cambiarEstado_TransicionValida_Retorna200() throws Exception {
        when(envioService.cambiarEstado(eq(1), any())).thenReturn(envioDTO(1, Envio.EN_TRANSITO));

        mockMvc.perform(patch("/api/envios/{id}/estado", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nuevoEstado\":\"EN_TRANSITO\",\"observaciones\":\"Salida a ruta\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoEnvio").value(Envio.EN_TRANSITO));
    }

    @Test
    @DisplayName("PATCH /api/envios/{id}/estado con una transicion invalida retorna 400 Bad Request")
    void cambiarEstado_TransicionInvalida_Retorna400() throws Exception {
        when(envioService.cambiarEstado(eq(1), any()))
                .thenThrow(new InvalidStateTransitionException("EXP-1001", "ENTREGADO es un estado final"));

        mockMvc.perform(patch("/api/envios/{id}/estado", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nuevoEstado\":\"EN_TRANSITO\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("PATCH /api/envios/{id}/estado con un estado fuera del catalogo retorna 400")
    void cambiarEstado_EstadoNoPermitido_Retorna400() throws Exception {
        mockMvc.perform(patch("/api/envios/{id}/estado", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nuevoEstado\":\"EXTRAVIADO\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nuevoEstado").exists());
    }

    @Test
    @DisplayName("PATCH /api/envios/vehiculo/{id}/estado retorna el conteo de envios actualizados")
    void cambiarEstadoMasivo_VehiculoExistente_Retorna200() throws Exception {
        when(envioService.actualizarEstadoMasivoPorVehiculo(eq(1), eq("CANCELADO"), any()))
                .thenReturn(3);

        mockMvc.perform(patch("/api/envios/vehiculo/{vehiculoId}/estado", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nuevoEstado\":\"CANCELADO\",\"observaciones\":\"Unidad en taller\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enviosActualizados").value(3))
                .andExpect(jsonPath("$.nuevoEstado").value(Envio.CANCELADO));
    }

    // Fabricas de datos de prueba

    private EnvioRequestDTO solicitudValida() {
        EnvioRequestDTO solicitud = new EnvioRequestDTO();
        solicitud.setCodigoRastreo("EXP-1001");
        solicitud.setDireccionDestino("Paraiso, Cartago");
        solicitud.setPesoKg(new BigDecimal("50.00"));
        solicitud.setCosto(new BigDecimal("7500.00"));
        solicitud.setVehiculoId(1);
        solicitud.setConductorId(1);
        return solicitud;
    }

    private EnvioResponseDTO envioDTO(Integer id, String estado) {
        return new EnvioResponseDTO(
                id,
                "EXP-100" + id,
                "Paraiso, Cartago",
                new BigDecimal("50.00"),
                new BigDecimal("7500.00"),
                estado,
                "CRC-1001",
                "Ana Morales Vargas",
                1,
                new BigDecimal("1000.00"),
                "DISPONIBLE",
                1,
                "Transportes Paraiso",
                1,
                "B1-334455",
                LocalDateTime.now(),
                LocalDateTime.now());
    }
}
