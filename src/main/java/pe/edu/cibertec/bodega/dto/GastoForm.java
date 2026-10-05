package pe.edu.cibertec.bodega.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;

@Data
public class GastoForm {
    @NotNull(message = "Seleccione una tienda.")
    private Integer idTienda;
    @NotNull(message = "Seleccione una categoría.")
    private Integer idCategoriaGasto;
    @NotNull(message = "Ingrese la fecha.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fecha = LocalDate.now(ZoneId.of("America/Lima"));
    @Size(max = 120, message = "La descripción admite hasta 120 caracteres.")
    private String descripcion;
    @NotNull(message = "Ingrese el importe.")
    @DecimalMin(value = "0.01", message = "El importe debe ser mayor que cero.")
    @Digits(integer = 8, fraction = 2, message = "Use hasta 8 dígitos enteros y 2 decimales.")
    private BigDecimal monto;
}
