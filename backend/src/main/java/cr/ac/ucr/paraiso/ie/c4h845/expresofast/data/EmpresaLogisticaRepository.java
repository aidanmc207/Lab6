package cr.ac.ucr.paraiso.ie.c4h845.expresofast.data;

import cr.ac.ucr.paraiso.ie.c4h845.expresofast.domain.EmpresaLogistica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmpresaLogisticaRepository extends JpaRepository<EmpresaLogistica, Integer> {

    Optional<EmpresaLogistica> findByCedulaJuridica(String cedulaJuridica);

    boolean existsByNombre(String nombre);
}
