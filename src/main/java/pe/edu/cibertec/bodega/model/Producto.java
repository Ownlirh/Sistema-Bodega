package pe.edu.cibertec.bodega.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

/**
 * Guarda los DOS precios a proposito: de precioVenta - precioCompra
 * sale el margen que alimenta el reporte de rentabilidad y el panel.
 */
@Entity
@Table(name = "tb_productos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_producto")
    private Integer idProducto;

    @Column(nullable = false, unique = true, length = 20)
    private String codigo;

    @Column(nullable = false, length = 80)
    private String descripcion;

    @Column(name = "unidad_medida", nullable = false, length = 15)
    private String unidadMedida = "UNIDAD";

    @Column(name = "precio_compra", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioCompra;

    @Column(name = "precio_venta", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioVenta;

    @Column(name = "stock_minimo", nullable = false)
    private Integer stockMinimo = 5;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_categoria", nullable = false)
    private Categoria categoria;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_proveedor")
    private Proveedor proveedor;

    @Column(nullable = false)
    private Boolean estado = true;

    /** Ganancia por unidad vendida. */
    @Transient
    public BigDecimal getMargen() {
        if (precioVenta == null || precioCompra == null) {
            return BigDecimal.ZERO;
        }
        return precioVenta.subtract(precioCompra);
    }
}
