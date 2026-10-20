package pe.edu.cibertec.bodega.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pe.edu.cibertec.bodega.model.Fiado;

public interface FiadoRepository extends JpaRepository<Fiado, Integer> {

    // Listar todos los fiados con sus clientes.
    @Query("""
           SELECT f
           FROM Fiado f
           JOIN FETCH f.cliente
           ORDER BY f.fecha DESC
           """)
    List<Fiado> findAllWithCliente();

    // Buscar un fiado con los datos de su cliente.
    @Query("""
           SELECT f
           FROM Fiado f
           JOIN FETCH f.cliente
           WHERE f.idFiado = :idFiado
           """)
    Optional<Fiado> findByIdWithCliente(
            @Param("idFiado") Integer idFiado);

    // Obtener todos los fiados de un cliente.
    @Query("""
           SELECT f
           FROM Fiado f
           JOIN FETCH f.cliente
           WHERE f.cliente.idCliente = :idCliente
           ORDER BY f.fecha DESC
           """)
    List<Fiado> findAllByClienteWithCliente(
            @Param("idCliente") Integer idCliente);

    // Obtener únicamente los fiados con saldo pendiente.
    @Query("""
           SELECT f
           FROM Fiado f
           JOIN FETCH f.cliente
           WHERE f.saldoPendiente > 0
           ORDER BY f.fecha DESC
           """)
    List<Fiado> findAllDeudasPendientes();
}