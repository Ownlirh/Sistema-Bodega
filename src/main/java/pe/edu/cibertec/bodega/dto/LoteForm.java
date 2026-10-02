package pe.edu.cibertec.bodega.dto;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
@Data
public class LoteForm {
    @NotNull(message = "Seleccione un producto.") private Integer idProducto;
    @NotNull(message = "Seleccione una tienda.") private Integer idTienda;
    @NotBlank(message = "Ingrese el código del lote.") @Size(max = 60) private String codigo;
    @NotNull(message = "Ingrese el vencimiento.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate vencimiento;
    @NotNull @Min(value = 1, message = "La cantidad debe ser mayor que cero.") private Integer cantidad;
}
