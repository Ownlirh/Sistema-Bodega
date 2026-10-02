package pe.edu.cibertec.bodega.model;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity @Table(name = "tb_lotes")
@Getter @Setter @NoArgsConstructor
public class Lote {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_lote") private Integer idLote;
    @ManyToOne @JoinColumn(name = "id_producto", nullable = false) private Producto producto;
    @ManyToOne @JoinColumn(name = "id_tienda", nullable = false) private Tienda tienda;
    @Column(nullable = false, length = 60) private String codigo;
    @Column(nullable = false) private LocalDate vencimiento;
    @Column(nullable = false) private Integer disponible;
    @Column(name = "cantidad_inicial", nullable = false) private Integer cantidadInicial;
    @Column(name = "fecha_registro", nullable = false) private LocalDateTime fechaRegistro;
    @ManyToOne @JoinColumn(name = "id_usuario", nullable = false) private Usuario usuario;
    @Version @Column(nullable = false) private Long version;
}
