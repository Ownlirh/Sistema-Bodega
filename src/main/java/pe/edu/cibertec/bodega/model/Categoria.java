package pe.edu.cibertec.bodega.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tb_categorias")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_categoria")
    private Integer idCategoria;

    @Column(nullable = false, length = 45)
    private String nombre;

    @Column(nullable = false)
    private Boolean estado = true;
}
