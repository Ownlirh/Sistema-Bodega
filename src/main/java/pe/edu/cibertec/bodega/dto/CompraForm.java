package pe.edu.cibertec.bodega.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class CompraForm {
    @NotNull private Integer idProveedor;
    @NotNull private Integer idTienda;
    @Valid @NotEmpty @Size(max = 100)
    private List<Linea> lineas = new ArrayList<>(List.of(new Linea()));

    @Data
    public static class Linea {
        @NotNull private Integer idProducto;
        @NotNull @Min(1) private Integer cantidad;
        @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2)
        private BigDecimal costo;
    }
}
