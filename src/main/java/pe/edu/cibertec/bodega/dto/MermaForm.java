package pe.edu.cibertec.bodega.dto;
import jakarta.validation.constraints.*;
import lombok.Data;
import pe.edu.cibertec.bodega.model.Merma.Motivo;
@Data
public class MermaForm {
    @NotNull(message = "Seleccione un lote.") private Integer idLote;
    @NotNull @Min(value = 1, message = "La cantidad debe ser mayor que cero.") private Integer cantidad;
    @NotNull(message = "Seleccione un motivo.") private Motivo motivo;
    @NotBlank(message = "Describa la causa de la merma.") @Size(max = 150) private String descripcion;
}
