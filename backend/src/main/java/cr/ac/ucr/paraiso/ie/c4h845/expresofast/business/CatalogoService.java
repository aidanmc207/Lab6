package cr.ac.ucr.paraiso.ie.c4h845.expresofast.business;

import cr.ac.ucr.paraiso.ie.c4h845.expresofast.data.ConductorRepository;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.data.EmpresaLogisticaRepository;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.data.VehiculoRepository;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.dto.EmpresaDTO;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.dto.ConductorDTO;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.dto.VehiculoDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Catalogos de apoyo que alimentan los desplegables del formulario de registro. */
@Service
@Transactional(readOnly = true)
public class CatalogoService {

    private final VehiculoRepository vehiculoRepository;
    private final ConductorRepository conductorRepository;
    private final EmpresaLogisticaRepository empresaRepository;

    public CatalogoService(VehiculoRepository vehiculoRepository,
                           ConductorRepository conductorRepository,
                           EmpresaLogisticaRepository empresaRepository) {
        this.vehiculoRepository = vehiculoRepository;
        this.conductorRepository = conductorRepository;
        this.empresaRepository = empresaRepository;
    }

    /** Usa JOIN FETCH para traer cada vehiculo con su empresa en una sola consulta. */
    public List<VehiculoDTO> listarVehiculos() {
        return vehiculoRepository.findAllConEmpresa().stream()
                .map(VehiculoDTO::desde)
                .toList();
    }

    public List<ConductorDTO> listarConductores() {
        return conductorRepository.findAll().stream()
                .map(ConductorDTO::desde)
                .toList();
    }

    public List<EmpresaDTO> listarEmpresas() {
        return empresaRepository.findAll().stream().map(EmpresaDTO::desde).toList();
    }
}
