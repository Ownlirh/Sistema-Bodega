package pe.edu.cibertec.bodega.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_auditoria")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Auditoria {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_auditoria")
    private Long idAuditoria;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(name = "id_usuario")
    private Integer idUsuario;

    @Column(name = "id_tienda")
    private Integer idTienda;

    @Column(nullable = false, length = 45)
    private String username;

    @Column(nullable = false, length = 80)
    private String accion;

    @Column(length = 500)
    private String detalle;

    @Column(name = "direccion_ip", length = 45)
    private String direccionIp;

    @Column(nullable = false, length = 15)
    private String resultado;
}
