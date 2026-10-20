
package pe.edu.cibertec.bodega.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tb_fiados")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Fiado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_fiado")
    private Integer idFiado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @Column(nullable = false, length = 200)
    private String descripcion;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(name = "monto_inicial", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoInicial;

    @Column(name = "saldo_pendiente", nullable = false, precision = 10, scale = 2)
    private BigDecimal saldoPendiente;

    @Column(nullable = false, length = 15)
    private String estado = "PENDIENTE";

    @PrePersist
    public void antesDeGuardar() {
        if (fecha == null) {
            fecha = LocalDateTime.now();
        }

        if (estado == null || estado.isBlank()) {
            estado = "PENDIENTE";
        }

        if (saldoPendiente == null) {
            saldoPendiente = montoInicial;
        }
    }
}
