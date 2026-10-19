package pe.edu.cibertec.bodega.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.cibertec.bodega.model.Cliente;

public interface ClienteRepository extends JpaRepository<Cliente, Integer> {

}