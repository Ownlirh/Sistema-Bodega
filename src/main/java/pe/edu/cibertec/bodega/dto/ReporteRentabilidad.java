package pe.edu.cibertec.bodega.dto;
import lombok.Value;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Value
public class ReporteRentabilidad {
    String tienda;
    List<Fila> filas;
    BigDecimal ingresos;
    BigDecimal costos;
    public BigDecimal getGanancia() { return ingresos.subtract(costos); }
    public BigDecimal getMargen() { return margen(ingresos, costos); }
    private static BigDecimal margen(BigDecimal ingreso, BigDecimal costo) {
        return ingreso.signum() == 0 ? null : ingreso.subtract(costo).multiply(BigDecimal.valueOf(100))
            .divide(ingreso, 2, RoundingMode.HALF_UP);
    }
    @Value
    public static class Fila {
        Integer idProducto;
        String codigo;
        String descripcion;
        Long unidades;
        BigDecimal ingresos;
        BigDecimal costos;
        public BigDecimal getGanancia() { return ingresos.subtract(costos); }
        public BigDecimal getMargen() { return margen(ingresos, costos); }
    }
}
