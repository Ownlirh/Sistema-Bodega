package pe.edu.cibertec.bodega.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.cibertec.bodega.model.CategoriaGasto;

public interface CategoriaGastoRepository extends JpaRepository<CategoriaGasto, Integer> {
}
