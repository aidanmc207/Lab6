package cr.ac.ucr.paraiso.ie.c4h845.expresofast.controller;

import cr.ac.ucr.paraiso.ie.c4h845.expresofast.business.VehiculoService;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.dto.VehiculoDTO;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.dto.VehiculoRequestDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/** Gestion completa de la flota: /api/vehiculos/** es territorio exclusivo de ROLE_ADMIN. */
@RestController
@RequestMapping("/api/vehiculos")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class VehiculoController {

    private final VehiculoService vehiculoService;

    public VehiculoController(VehiculoService vehiculoService) {
        this.vehiculoService = vehiculoService;
    }

    @GetMapping
    public ResponseEntity<List<VehiculoDTO>> listar() {
        return ResponseEntity.ok(vehiculoService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehiculoDTO> buscarPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(vehiculoService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<VehiculoDTO> registrar(@Valid @RequestBody VehiculoRequestDTO solicitud) {
        VehiculoDTO creado = vehiculoService.registrar(solicitud);
        return ResponseEntity.created(URI.create("/api/vehiculos/" + creado.id())).body(creado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<VehiculoDTO> actualizar(@PathVariable Integer id,
                                                  @Valid @RequestBody VehiculoRequestDTO solicitud) {
        return ResponseEntity.ok(vehiculoService.actualizar(id, solicitud));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        vehiculoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
