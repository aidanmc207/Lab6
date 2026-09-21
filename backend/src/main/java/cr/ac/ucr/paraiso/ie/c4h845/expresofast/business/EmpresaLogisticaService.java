package cr.ac.ucr.paraiso.ie.c4h845.expresofast.business;

import cr.ac.ucr.paraiso.ie.c4h845.expresofast.data.EmpresaLogisticaRepository;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.domain.EmpresaLogistica;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.dto.EmpresaDTO;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.exception.ReglaNegocioException;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Lab 7: gestion de empresas logisticas, tercer servicio certificado por la suite de pruebas. */
@Service
@Transactional(readOnly = true)
public class EmpresaLogisticaService {

    private final EmpresaLogisticaRepository empresaRepository;

    public EmpresaLogisticaService(EmpresaLogisticaRepository empresaRepository) {
        this.empresaRepository = empresaRepository;
    }

    public List<EmpresaDTO> listar() {
        return empresaRepository.findAll().stream().map(EmpresaDTO::desde).toList();
    }

    public EmpresaDTO buscarPorId(Integer id) {
        return EmpresaDTO.desde(obtenerOFallar(id));
    }

    public EmpresaDTO buscarPorCedulaJuridica(String cedulaJuridica) {
        return empresaRepository.findByCedulaJuridica(cedulaJuridica.trim())
                .map(EmpresaDTO::desde)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe la empresa con cedula juridica " + cedulaJuridica));
    }

    @Transactional
    public EmpresaDTO registrar(EmpresaLogistica solicitud) {
        String nombre = solicitud.getNombre().trim();

        if (empresaRepository.existsByNombre(nombre)) {
            throw new ReglaNegocioException("Ya existe una empresa registrada con el nombre " + nombre);
        }

        empresaRepository.findByCedulaJuridica(solicitud.getCedulaJuridica().trim())
                .ifPresent(existente -> {
                    throw new ReglaNegocioException("Ya existe una empresa con la cedula juridica "
                            + existente.getCedulaJuridica());
                });

        solicitud.setNombre(nombre);
        return EmpresaDTO.desde(empresaRepository.save(solicitud));
    }

    @Transactional
    public void eliminar(Integer id) {
        EmpresaLogistica empresa = obtenerOFallar(id);

        if (!empresa.getVehiculos().isEmpty()) {
            throw new ReglaNegocioException("La empresa " + empresa.getNombre()
                    + " tiene vehiculos asociados y no puede eliminarse");
        }
        empresaRepository.delete(empresa);
    }

    private EmpresaLogistica obtenerOFallar(Integer id) {
        return empresaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe la empresa con id " + id));
    }
}
