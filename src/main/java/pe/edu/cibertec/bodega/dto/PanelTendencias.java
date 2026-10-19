package pe.edu.cibertec.bodega.dto;

import lombok.Value;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Datos del panel de tendencias (integrante 1).
 *
 * Cruza las tres series mensuales del negocio en una sola tabla para que cada
 * fila compare el mismo mes:
 *
 *   ingresos        lo vendido            (VentaRepository.totalPorMes)
 *   mercaderia      lo comprado           (CompraRepository.totalPorMes)
 *   gastos          gastos operativos     (GastoRepository.totalPorMes)
 *
 * El margen es lo que queda: ingresos - mercaderia - gastos. Es margen de
 * caja, no contable: no descuenta mermas ni depreciacion.
 */
@Value
public class PanelTendencias {

    /** Nombre de la tienda filtrada, o "Todas las tiendas". */
    String tienda;

    /** Una fila por mes con movimiento, de mas viejo a mas nuevo. */
    List<Mes> meses;

    /** En que se va la plata. Respeta el filtro de tienda. */
    List<Rubro> gastosPorCategoria;

    /**
     * Comparativo entre tiendas. OJO: esta serie NO respeta el filtro, porque
     * su gracia es justamente comparar una tienda contra las otras.
     */
    List<Rubro> ventasPorTienda;

    public BigDecimal getIngresos() {
        return total(Mes::getIngresos);
    }

    public BigDecimal getMercaderia() {
        return total(Mes::getMercaderia);
    }

    public BigDecimal getGastos() {
        return total(Mes::getGastos);
    }

    public BigDecimal getEgresos() {
        return getMercaderia().add(getGastos());
    }

    public BigDecimal getMargen() {
        return getIngresos().subtract(getEgresos());
    }

    /** Null cuando no hubo ingresos: dividir por cero no es cero. */
    public BigDecimal getMargenPorcentaje() {
        return porcentaje(getMargen(), getIngresos());
    }

    public boolean isVacio() {
        return meses.isEmpty();
    }

    /**
     * El valor mas alto de todo el grafico. Las barras se dimensionan contra
     * el, asi la mas alta llena la fila y el resto queda en proporcion.
     */
    public BigDecimal getTecho() {
        return meses.stream()
                .map(m -> m.getIngresos().max(m.getEgresos()))
                .reduce(BigDecimal.ZERO, BigDecimal::max);
    }

    /** Ancho de barra en porcentaje del techo. Lo usa la plantilla. */
    public int ancho(BigDecimal valor) {
        BigDecimal techo = getTecho();
        if (valor == null || valor.signum() <= 0 || techo.signum() == 0) return 0;
        return valor.multiply(CIEN).divide(techo, 0, RoundingMode.HALF_UP).intValue();
    }

    private BigDecimal total(java.util.function.Function<Mes, BigDecimal> campo) {
        return meses.stream().map(campo).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    static BigDecimal porcentaje(BigDecimal parte, BigDecimal sobre) {
        return sobre == null || sobre.signum() == 0
                ? null
                : parte.multiply(CIEN).divide(sobre, 2, RoundingMode.HALF_UP);
    }

    /** Un mes del grafico de tendencias. */
    @Value
    public static class Mes {
        int anio;
        int mes;
        BigDecimal ingresos;
        BigDecimal mercaderia;
        BigDecimal gastos;

        public BigDecimal getEgresos() {
            return mercaderia.add(gastos);
        }

        public BigDecimal getMargen() {
            return ingresos.subtract(getEgresos());
        }

        public BigDecimal getMargenPorcentaje() {
            return porcentaje(getMargen(), ingresos);
        }

        /** Etiqueta corta para el eje, por ejemplo "09/2026". */
        public String getEtiqueta() {
            return String.format("%02d/%d", mes, anio);
        }
    }

    /**
     * Una barra de los graficos de participacion: gastos por categoria y
     * ventas por tienda.
     */
    @Value
    public static class Rubro {
        String nombre;
        BigDecimal total;
        /** Participacion sobre el total de su propia lista, ya redondeada. */
        int participacion;
    }
}
