package pe.edu.cibertec.bodega.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Inventario POR TIENDA. Cada bodega maneja su propio stock.
 *
 * El campo version implementa el bloqueo optimista de JPA. Si dos cajas
 * venden la misma ultima unidad al mismo tiempo, la segunda transaccion
 * falla con OptimisticLockException en lugar de dejar el stock en negativo.
 * Es el punto clave del caso transaccional: no quitar esta anotacion.
 */
@Entity
@Table(name = "tb_stock",
       uniqueConstraints = @UniqueConstraint(columnNames = {"id_producto", "id_tienda"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_stock")
    private Integer idStock;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_tienda", nullable = false)
    private Tienda tienda;

    @Column(nullable = false)
    private Integer cantidad = 0;

    @Version
    @Column(nullable = false)
    private Long version;
}
