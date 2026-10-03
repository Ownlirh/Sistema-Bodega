package pe.edu.cibertec.bodega.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tb_clientes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cliente")
    private Integer idCliente;

    @Column(nullable = false, length = 60)
    private String nombres;

    @Column(nullable = false, length = 60)
    private String apellidos;

    @Column(length = 8)
    private String dni;

    @Column(length = 15)
    private String telefono;

    @Column(length = 120)
    private String direccion;

    @Column(nullable = false)
    private Boolean estado = true;
}