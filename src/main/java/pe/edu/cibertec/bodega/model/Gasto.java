package pe.edu.cibertec.bodega.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Gasto operativo: alquiler, luz, sueldos, transporte.
 *
 * Tabla simple a proposito. Esto NO es un modulo contable:
 * solo fecha, categoria y monto, que es lo que necesita el panel.
 */
@Entity
@Table(name = "tb_gastos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Gasto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_gasto")
    private Integer idGasto;

    @Column(nullable = false)
    private LocalDate fecha;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_tienda", nullable = false)
    private Tienda tienda;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_categoria_gasto", nullable = false)
    private CategoriaGasto categoriaGasto;

    @Column(length = 120)
    private String descripcion;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;
}
