package cr.ac.ucr.paraiso.ie.c4h845.expresofast.data;

import cr.ac.ucr.paraiso.ie.c4h845.expresofast.domain.Envio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface EnvioRepository extends JpaRepository<Envio, Integer> {

    /** Elimina el N+1: las 4 tablas viajan en un unico SELECT (LEFT para no perder FK nulas). */
    @Query("SELECT DISTINCT e FROM Envio e "
            + "LEFT JOIN FETCH e.vehiculo v "
            + "LEFT JOIN FETCH v.empresa emp "
            + "LEFT JOIN FETCH e.conductor c "
            + "ORDER BY e.id DESC")
    List<Envio> findAllOptimizado();

    /** Misma optimizacion, filtrando por estado desde el panel lateral del tablero. */
    @Query("SELECT DISTINCT e FROM Envio e "
            + "LEFT JOIN FETCH e.vehiculo v "
            + "LEFT JOIN FETCH v.empresa emp "
            + "LEFT JOIN FETCH e.conductor c "
            + "WHERE e.estadoEnvio = :estado "
            + "ORDER BY e.id DESC")
    List<Envio> findByEstadoOptimizado(@Param("estado") String estado);

    /** Un unico envio con todo su grafo cargado (para la respuesta del POST/PATCH). */
    @Query("SELECT e FROM Envio e "
            + "LEFT JOIN FETCH e.vehiculo v "
            + "LEFT JOIN FETCH v.empresa emp "
            + "LEFT JOIN FETCH e.conductor c "
            + "WHERE e.id = :id")
    Optional<Envio> findByIdOptimizado(@Param("id") Integer id);

    Optional<Envio> findByCodigoRastreo(String codigoRastreo);

    boolean existsByCodigoRastreo(String codigoRastreo);

    /** Envios de un vehiculo; se recorren para auditar el estado previo antes del UPDATE masivo. */
    List<Envio> findByVehiculoId(Integer vehiculoId);

    /** Actualizacion masiva; clearAutomatically evita que queden entidades obsoletas en memoria. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Envio e SET e.estadoEnvio = :estado WHERE e.vehiculo.id = :vehiculoId")
    int actualizarEstadoMasivoPorVehiculo(@Param("vehiculoId") Integer vehiculoId,
                                          @Param("estado") String estado);

    /** Carga (kg) ya comprometida en un vehiculo: envios ni entregados ni cancelados. */
    @Query("SELECT COALESCE(SUM(e.pesoKg), 0) FROM Envio e "
            + "WHERE e.vehiculo.id = :vehiculoId "
            + "AND e.estadoEnvio IN ('PENDIENTE', 'EN_TRANSITO')")
    BigDecimal sumarPesoActivoPorVehiculo(@Param("vehiculoId") Integer vehiculoId);

    long countByEstadoEnvio(String estadoEnvio);
}
