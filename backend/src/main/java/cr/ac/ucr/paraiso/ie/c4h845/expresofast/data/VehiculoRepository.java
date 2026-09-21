package cr.ac.ucr.paraiso.ie.c4h845.expresofast.data;

import cr.ac.ucr.paraiso.ie.c4h845.expresofast.domain.Vehiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehiculoRepository extends JpaRepository<Vehiculo, Integer> {

    Optional<Vehiculo> findByPlaca(String placa);

    List<Vehiculo> findByEstado(String estado);

    /** Flota operativa: todo lo que no esta detenido en mantenimiento. */
    long countByEstadoNot(String estado);

    /** JOIN FETCH para el catalogo: trae el vehiculo junto a su empresa duena. */
    @Query("SELECT v FROM Vehiculo v LEFT JOIN FETCH v.empresa ORDER BY v.placa ASC")
    List<Vehiculo> findAllConEmpresa();
}
