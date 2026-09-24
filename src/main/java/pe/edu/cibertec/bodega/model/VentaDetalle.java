package pe.edu.cibertec.bodega.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

/**
 * precioUnitario se copia del producto al momento de vender.
 * Si manana sube el precio, la venta de ayer no debe cambiar.
 */
@Entity
@Table(name = "tb_venta_detalle")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class VentaDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle")
    private Integer idDetalle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_venta", nullable = false)
    private Venta venta;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;
}
