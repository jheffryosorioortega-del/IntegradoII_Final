package pe.sgp.clientes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.sgp.clientes.model.Cliente;

import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findFirstByTelefono(String telefono);
    List<Cliente> findAllByOrderByNombreAsc();
}
