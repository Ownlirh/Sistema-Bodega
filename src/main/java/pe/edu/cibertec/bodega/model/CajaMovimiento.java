package pe.edu.cibertec.bodega.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_caja_movimientos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CajaMovimiento {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_movimiento")
    private Long idMovimiento;

    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "id_caja", nullable = false)
    private Caja caja;

    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(nullable = false, length = 10)
    private String tipo;

    @Column(nullable = false, length = 120)
    private String concepto;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;
}
