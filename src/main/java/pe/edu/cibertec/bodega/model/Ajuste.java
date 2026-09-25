package pe.edu.cibertec.bodega.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Ajuste de inventario por conteo fisico (integrante 7).
 * stockSistema = lo que decia tb_stock
 * stockFisico  = lo que se conto en el estante
 * diferencia   = stockFisico - stockSistema (puede ser negativa)
 */
@Entity
@Table(name = "tb_ajustes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Ajuste {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ajuste")
    private Integer idAjuste;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_tienda", nullable = false)
    private Tienda tienda;

    @Column(name = "stock_sistema", nullable = false)
    private Integer stockSistema;

    @Column(name = "stock_fisico", nullable = false)
    private Integer stockFisico;

    @Column(nullable = false)
    private Integer diferencia;

    @Column(nullable = false, length = 150)
    private String motivo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;
}
