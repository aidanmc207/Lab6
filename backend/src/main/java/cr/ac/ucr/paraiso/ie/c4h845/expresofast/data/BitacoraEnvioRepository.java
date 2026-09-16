package cr.ac.ucr.paraiso.ie.c4h845.expresofast.data;

import cr.ac.ucr.paraiso.ie.c4h845.expresofast.domain.BitacoraEnvio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BitacoraEnvioRepository extends JpaRepository<BitacoraEnvio, Integer> {

    @Query("SELECT b FROM BitacoraEnvio b "
            + "JOIN FETCH b.usuario u "
            + "WHERE b.envio.id = :envioId "
            + "ORDER BY b.fechaCambio DESC, b.id DESC")
    List<BitacoraEnvio> findByEnvioOptimizado(@Param("envioId") Integer envioId);

    long countByEnvioId(Integer envioId);
}
