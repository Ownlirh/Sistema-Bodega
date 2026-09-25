package pe.edu.cibertec.bodega.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Kardex: historial de movimientos de stock (integrante 7).
 * Cada fila guarda cuanto habia ANTES y cuanto quedo DESPUES del movimiento.
 *
 * tipo: VENTA, COMPRA, DEVOLUCION, TRANSFERENCIA_SALIDA,
 *       TRANSFERENCIA_ENTRADA o AJUSTE
 * cantidad: positiva si entra mercaderia, negativa si sale.
 */
@Entity
@Table(name = "tb_kardex")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Kardex {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_kardex")
    private Integer idKardex;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_tienda", nullable = false)
    private Tienda tienda;

    @Column(nullable = false, length = 25)
    private String tipo;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "stock_antes", nullable = false)
    private Integer stockAntes;

    @Column(name = "stock_despues", nullable = false)
    private Integer stockDespues;

    @Column(length = 100)
    private String referencia;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;
}
