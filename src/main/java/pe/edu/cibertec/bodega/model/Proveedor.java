package pe.edu.cibertec.bodega.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tb_proveedores")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Proveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_proveedor")
    private Integer idProveedor;

    @Column(name = "razon_social", nullable = false, length = 80)
    private String razonSocial;

    @Column(length = 11)
    private String ruc;

    @Column(length = 15)
    private String telefono;

    @Column(length = 80)
    private String email;

    @Column(nullable = false)
    private Boolean estado = true;
}
