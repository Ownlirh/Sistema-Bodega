package pe.edu.cibertec.bodega.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tb_tiendas")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Tienda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tienda")
    private Integer idTienda;

    @Column(nullable = false, length = 60)
    private String nombre;

    @Column(length = 120)
    private String direccion;

    @Column(length = 60)
    private String distrito;

    @Column(length = 15)
    private String telefono;

    @Column(nullable = false)
    private Boolean estado = true;
}
