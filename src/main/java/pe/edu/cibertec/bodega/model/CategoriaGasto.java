package pe.edu.cibertec.bodega.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tb_categorias_gasto")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CategoriaGasto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_categoria_gasto")
    private Integer idCategoriaGasto;

    @Column(nullable = false, length = 45)
    private String nombre;
}
