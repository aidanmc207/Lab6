package cr.ac.ucr.paraiso.ie.c4h845.expresofast.data;

import cr.ac.ucr.paraiso.ie.c4h845.expresofast.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    /** JOIN FETCH de roles: el UserDetailsService los necesita fuera de la transaccion. */
    @Query("SELECT u FROM Usuario u LEFT JOIN FETCH u.roles WHERE u.username = :username")
    Optional<Usuario> findByUsername(@Param("username") String username);

    boolean existsByUsername(String username);
}
