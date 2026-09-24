package pe.edu.cibertec.bodega.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tb_roles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Rol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_rol")
    private Integer idRol;

    @Column(nullable = false, unique = true, length = 30)
    private String nombre;
}
