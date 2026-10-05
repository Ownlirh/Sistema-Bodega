package pe.edu.cibertec.bodega.dto;
import lombok.Value;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
@Value
public class ReporteVentas {
    LocalDate desde;
    LocalDate hasta;
    String tienda;
    List<Fila> filas;
    BigDecimal total;
    @Value
    public static class Fila {
        Integer numero;
        LocalDateTime fecha;
        String tienda;
        String vendedor;
        BigDecimal total;
    }
}
