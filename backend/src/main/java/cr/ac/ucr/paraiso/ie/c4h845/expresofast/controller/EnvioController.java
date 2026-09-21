package cr.ac.ucr.paraiso.ie.c4h845.expresofast.controller;

import cr.ac.ucr.paraiso.ie.c4h845.expresofast.business.EnvioService;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.dto.BitacoraResponseDTO;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.dto.CambioEstadoDTO;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.dto.EnvioRequestDTO;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.dto.EnvioResponseDTO;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.dto.ResumenEnviosDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/envios")
public class EnvioController {

    private final EnvioService envioService;

    public EnvioController(EnvioService envioService) {
        this.envioService = envioService;
    }

    /** GET /api/envios/optimizados -> lista cargada con JOIN FETCH (sin N+1). */
    @GetMapping("/optimizados")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_OPERADOR','ROLE_CONDUCTOR')")
    public ResponseEntity<List<EnvioResponseDTO>> listarOptimizados() {
        return ResponseEntity.ok(envioService.listarOptimizados());
    }

    /** GET /api/envios?estado=PENDIENTE -> mismo JOIN FETCH filtrado por estado. */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_OPERADOR','ROLE_CONDUCTOR')")
    public ResponseEntity<List<EnvioResponseDTO>> listar(@RequestParam(required = false) String estado) {
        if (estado == null || estado.isBlank() || "TODOS".equalsIgnoreCase(estado)) {
            return ResponseEntity.ok(envioService.listarOptimizados());
        }
        return ResponseEntity.ok(envioService.listarPorEstado(estado.toUpperCase()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_OPERADOR','ROLE_CONDUCTOR')")
    public ResponseEntity<EnvioResponseDTO> buscarPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(envioService.buscarPorId(id));
    }

    /** GET /api/envios/resumen -> contadores por estado para las tarjetas KPI. */
    @GetMapping("/resumen")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_OPERADOR','ROLE_CONDUCTOR')")
    public ResponseEntity<ResumenEnviosDTO> resumen() {
        return ResponseEntity.ok(envioService.resumen());
    }

    /** GET /api/envios/{id}/bitacora -> historial de auditoria del envio. */
    @GetMapping("/{id}/bitacora")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_OPERADOR')")
    public ResponseEntity<List<BitacoraResponseDTO>> bitacora(@PathVariable Integer id) {
        return ResponseEntity.ok(envioService.listarBitacora(id));
    }

    /** POST /api/envios -> registra un nuevo envio express. */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_OPERADOR')")
    public ResponseEntity<EnvioResponseDTO> registrar(@Valid @RequestBody EnvioRequestDTO solicitud) {
        EnvioResponseDTO creado = envioService.registrar(solicitud);
        return ResponseEntity.created(URI.create("/api/envios/" + creado.id())).body(creado);
    }

    /**
     * PUT/PATCH /api/envios/{id}/estado -> cambia el estado y genera la bitacora.
     * Cada rol solo puede provocar la transicion que le corresponde en la operacion:
     * el operador despacha a EN_TRANSITO, el conductor confirma la ENTREGA y el
     * administrador puede aplicar cualquiera, incluida la cancelacion.
     */
    @RequestMapping(value = "/{id}/estado", method = {RequestMethod.PUT, RequestMethod.PATCH})
    @PreAuthorize("hasAuthority('ROLE_ADMIN') "
            + "or (hasAuthority('ROLE_OPERADOR') and #cambio.nuevoEstado == 'EN_TRANSITO') "
            + "or (hasAuthority('ROLE_CONDUCTOR') and #cambio.nuevoEstado == 'ENTREGADO')")
    public ResponseEntity<EnvioResponseDTO> cambiarEstado(@PathVariable Integer id,
                                                          @Valid @RequestBody CambioEstadoDTO cambio) {
        return ResponseEntity.ok(envioService.cambiarEstado(id, cambio));
    }

    /** PATCH masivo por vehiculo, respaldado por la consulta @Modifying del repositorio. */
    @PatchMapping("/vehiculo/{vehiculoId}/estado")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> cambiarEstadoMasivo(
            @PathVariable Integer vehiculoId,
            @Valid @RequestBody CambioEstadoDTO cambio) {

        int afectados = envioService.actualizarEstadoMasivoPorVehiculo(
                vehiculoId, cambio.getNuevoEstado(), cambio.getObservaciones());

        return ResponseEntity.ok(Map.of(
                "vehiculoId", vehiculoId,
                "nuevoEstado", cambio.getNuevoEstado().toUpperCase(),
                "enviosActualizados", afectados));
    }
}
