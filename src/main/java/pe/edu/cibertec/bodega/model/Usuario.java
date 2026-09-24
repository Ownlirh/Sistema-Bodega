package pe.edu.cibertec.bodega.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Empleado del sistema.
 * El rol define QUE puede hacer; la tienda define QUE DATOS ve.
 * El administrador lleva tienda == null porque ve todas.
 */
@Entity
@Table(name = "tb_usuarios")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Integer idUsuario;

    @Column(nullable = false, unique = true, length = 45)
    private String username;

    @Column(nullable = false, length = 100)
    private String password;

    @Column(nullable = false, length = 60)
    private String nombres;

    @Column(nullable = false, length = 60)
    private String apellidos;

    @Column(length = 80)
    private String email;

    @Column(length = 8)
    private String dni;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_rol", nullable = false)
    private Rol rol;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_tienda")
    private Tienda tienda;

    @Column(nullable = false)
    private Boolean estado = true;

    @Transient
    public String getNombreCompleto() {
        return nombres + " " + apellidos;
    }
}
