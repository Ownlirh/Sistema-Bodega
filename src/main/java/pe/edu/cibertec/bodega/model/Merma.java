package pe.edu.cibertec.bodega.model;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "tb_mermas")
@Getter @Setter @NoArgsConstructor
public class Merma {
    public enum Motivo { VENCIMIENTO, DANO, ROTURA, OTRO }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_merma") private Integer idMerma;
    @ManyToOne @JoinColumn(name = "id_lote", nullable = false) private Lote lote;
    @Column(nullable = false) private LocalDateTime fecha;
    @Column(nullable = false) private Integer cantidad;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Motivo motivo;
    @Column(nullable = false, length = 150) private String descripcion;
    @ManyToOne @JoinColumn(name = "id_usuario", nullable = false) private Usuario usuario;
}
