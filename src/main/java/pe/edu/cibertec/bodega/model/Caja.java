package pe.edu.cibertec.bodega.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_cajas")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Caja {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_caja")
    private Long idCaja;

    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "id_tienda", nullable = false)
    private Tienda tienda;

    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "id_usuario_apertura", nullable = false)
    private Usuario usuarioApertura;

    @Column(name = "fecha_apertura", nullable = false)
    private LocalDateTime fechaApertura;

    @Column(name = "monto_inicial", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoInicial;

    @Column(nullable = false, length = 15)
    private String estado = "ABIERTA";

    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "id_usuario_cierre")
    private Usuario usuarioCierre;

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    @Column(name = "total_ventas", precision = 12, scale = 2)
    private BigDecimal totalVentas;

    @Column(name = "total_ingresos", precision = 12, scale = 2)
    private BigDecimal totalIngresos;

    @Column(name = "total_egresos", precision = 12, scale = 2)
    private BigDecimal totalEgresos;

    @Column(name = "efectivo_esperado", precision = 12, scale = 2)
    private BigDecimal efectivoEsperado;

    @Column(name = "efectivo_contado", precision = 12, scale = 2)
    private BigDecimal efectivoContado;

    @Column(precision = 12, scale = 2)
    private BigDecimal diferencia;

    @Column(name = "observacion_cierre", length = 500)
    private String observacionCierre;
}
