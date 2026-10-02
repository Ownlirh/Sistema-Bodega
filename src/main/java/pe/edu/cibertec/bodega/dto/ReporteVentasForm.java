package pe.edu.cibertec.bodega.dto;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.*;
@Data
public class ReporteVentasForm {
    @NotNull(message = "Ingrese la fecha inicial.") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate desde = LocalDate.now(ZoneId.of("America/Lima")).withDayOfMonth(1);
    @NotNull(message = "Ingrese la fecha final.") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate hasta = LocalDate.now(ZoneId.of("America/Lima"));
    private Integer idTienda;
}
