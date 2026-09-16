package cr.ac.ucr.paraiso.ie.c4h845.expresofast.business;

import cr.ac.ucr.paraiso.ie.c4h845.expresofast.data.EmpresaLogisticaRepository;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.data.EnvioRepository;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.data.VehiculoRepository;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.domain.EmpresaLogistica;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.domain.Vehiculo;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.dto.VehiculoDTO;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.dto.VehiculoRequestDTO;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.exception.ReglaNegocioException;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Gestion completa de la flota; el acceso queda restringido a ROLE_ADMIN. */
@Service
@Transactional(readOnly = true)
public class VehiculoService {

    private final VehiculoRepository vehiculoRepository;
    private final EmpresaLogisticaRepository empresaRepository;
    private final EnvioRepository envioRepository;

    public VehiculoService(VehiculoRepository vehiculoRepository,
                           EmpresaLogisticaRepository empresaRepository,
                           EnvioRepository envioRepository) {
        this.vehiculoRepository = vehiculoRepository;
        this.empresaRepository = empresaRepository;
        this.envioRepository = envioRepository;
    }

    public List<VehiculoDTO> listar() {
        return vehiculoRepository.findAllConEmpresa().stream()
                .map(VehiculoDTO::desde)
                .toList();
    }

    public VehiculoDTO buscarPorId(Integer id) {
        return VehiculoDTO.desde(obtenerOFallar(id));
    }

    @Transactional
    public VehiculoDTO registrar(VehiculoRequestDTO solicitud) {
        String placa = solicitud.getPlaca().trim().toUpperCase();

        if (vehiculoRepository.findByPlaca(placa).isPresent()) {
            throw new ReglaNegocioException("Ya existe un vehiculo con la placa " + placa);
        }

        Vehiculo vehiculo = new Vehiculo();
        aplicar(vehiculo, solicitud, placa);

        return VehiculoDTO.desde(vehiculoRepository.save(vehiculo));
    }

    @Transactional
    public VehiculoDTO actualizar(Integer id, VehiculoRequestDTO solicitud) {
        Vehiculo vehiculo = obtenerOFallar(id);
        String placa = solicitud.getPlaca().trim().toUpperCase();

        vehiculoRepository.findByPlaca(placa)
                .filter(existente -> !existente.getId().equals(id))
                .ifPresent(existente -> {
                    throw new ReglaNegocioException("Ya existe otro vehiculo con la placa " + placa);
                });

        aplicar(vehiculo, solicitud, placa);
        return VehiculoDTO.desde(vehiculo); // dirty checking: el UPDATE sale en el commit
    }

    @Transactional
    public void eliminar(Integer id) {
        Vehiculo vehiculo = obtenerOFallar(id);

        if (!envioRepository.findByVehiculoId(id).isEmpty()) {
            throw new ReglaNegocioException("El vehiculo " + vehiculo.getPlaca()
                    + " tiene envios asociados y no puede eliminarse");
        }
        vehiculoRepository.delete(vehiculo);
    }

    private void aplicar(Vehiculo vehiculo, VehiculoRequestDTO solicitud, String placa) {
        EmpresaLogistica empresa = empresaRepository.findById(solicitud.getEmpresaId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe la empresa con id " + solicitud.getEmpresaId()));

        vehiculo.setPlaca(placa);
        vehiculo.setCapacidadKg(solicitud.getCapacidadKg());
        vehiculo.setEstado(solicitud.getEstado().trim().toUpperCase());
        vehiculo.setEmpresa(empresa);
    }

    private Vehiculo obtenerOFallar(Integer id) {
        return vehiculoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el vehiculo con id " + id));
    }
}
