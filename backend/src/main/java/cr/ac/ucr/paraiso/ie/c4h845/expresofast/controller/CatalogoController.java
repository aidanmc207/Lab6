package cr.ac.ucr.paraiso.ie.c4h845.expresofast.controller;

import cr.ac.ucr.paraiso.ie.c4h845.expresofast.business.CatalogoService;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.dto.ConductorDTO;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.dto.EmpresaDTO;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.dto.VehiculoDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Catalogos de solo lectura que alimentan los desplegables del tablero autenticado. */
@RestController
@RequestMapping("/api/catalogos")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_OPERADOR','ROLE_CONDUCTOR')")
public class CatalogoController {

    private final CatalogoService catalogoService;

    public CatalogoController(CatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    @GetMapping("/vehiculos")
    public ResponseEntity<List<VehiculoDTO>> vehiculos() {
        return ResponseEntity.ok(catalogoService.listarVehiculos());
    }

    @GetMapping("/conductores")
    public ResponseEntity<List<ConductorDTO>> conductores() {
        return ResponseEntity.ok(catalogoService.listarConductores());
    }

    @GetMapping("/empresas")
    public ResponseEntity<List<EmpresaDTO>> empresas() {
        return ResponseEntity.ok(catalogoService.listarEmpresas());
    }
}
