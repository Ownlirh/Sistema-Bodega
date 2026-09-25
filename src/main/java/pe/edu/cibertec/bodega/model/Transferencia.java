package pe.edu.cibertec.bodega.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Transferencia de stock entre tiendas (integrante 7).
 * Version simple: una transferencia mueve UN producto de la tienda
 * origen a la tienda destino.
 */
@Entity
@Table(name = "tb_transferencias")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Transferencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_transferencia")
    private Integer idTransferencia;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_tienda_origen", nullable = false)
    private Tienda tiendaOrigen;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_tienda_destino", nullable = false)
    private Tienda tiendaDestino;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(length = 150)
    private String observacion;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;
}
