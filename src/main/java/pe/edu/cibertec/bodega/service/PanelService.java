package pe.edu.cibertec.bodega.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.cibertec.bodega.dto.PanelTendencias;
import pe.edu.cibertec.bodega.repository.CompraRepository;
import pe.edu.cibertec.bodega.repository.GastoRepository;
import pe.edu.cibertec.bodega.repository.TiendaRepository;
import pe.edu.cibertec.bodega.repository.VentaRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Arma el panel de tendencias (integrante 1).
 *
 * Las consultas ya existian en los repositorios; lo que faltaba era cruzarlas.
 * Cada una devuelve sus propios meses, y no tienen por que coincidir: un mes
 * puede tener gastos de luz y ninguna venta. Si se mostraran una al lado de la
 * otra sin cruzarlas, las filas no compararian el mismo periodo.
 */
@Service
@RequiredArgsConstructor
public class PanelService {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    /** Posicion de cada serie dentro del acumulador por mes. */
    private static final int INGRESOS = 0;
    private static final int MERCADERIA = 1;
    private static final int GASTOS = 2;

    private final VentaRepository ventas;
    private final CompraRepository compras;
    private final GastoRepository gastos;
    private final TiendaRepository tiendas;

    /**
     * @param idTienda null para ver todas las tiendas juntas.
     * @throws IllegalArgumentException si la tienda no existe.
     */
    @Transactional(readOnly = true)
    public PanelTendencias consultar(Integer idTienda) {
        String tienda = idTienda == null
                ? "Todas las tiendas"
                : tiendas.findById(idTienda)
                    .orElseThrow(() -> new IllegalArgumentException("La tienda no existe."))
                    .getNombre();

        // TreeMap: los meses salen ordenados sin tener que ordenarlos despues.
        Map<YearMonth, BigDecimal[]> porMes = new TreeMap<>();
        acumular(porMes, ventas.totalPorMes(idTienda), INGRESOS);
        acumular(porMes, compras.totalPorMes(idTienda), MERCADERIA);
        acumular(porMes, gastos.totalPorMes(idTienda), GASTOS);

        List<PanelTendencias.Mes> meses = porMes.entrySet().stream()
                .map(e -> new PanelTendencias.Mes(
                        e.getKey().getYear(),
                        e.getKey().getMonthValue(),
                        e.getValue()[INGRESOS],
                        e.getValue()[MERCADERIA],
                        e.getValue()[GASTOS]))
                .toList();

        return new PanelTendencias(
                tienda,
                meses,
                rubros(gastos.totalPorCategoria(idTienda)),
                // Sin filtro a proposito: el comparativo entre tiendas pierde
                // sentido si se restringe a una sola.
                rubros(ventas.totalPorTienda()));
    }

    /**
     * Vuelca una serie [anio, mes, total] en la columna que le corresponde del
     * acumulador, creando el mes si es la primera serie que lo trae.
     */
    private void acumular(Map<YearMonth, BigDecimal[]> destino, List<Object[]> filas, int columna) {
        for (Object[] fila : filas) {
            if (fila[0] == null || fila[1] == null) continue;
            YearMonth clave = YearMonth.of(((Number) fila[0]).intValue(), ((Number) fila[1]).intValue());
            BigDecimal[] acumulado = destino.computeIfAbsent(clave,
                    k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
            acumulado[columna] = acumulado[columna].add(cero(fila[2]));
        }
    }

    /**
     * Convierte una serie [nombre, total] en barras, calculando de paso cuanto
     * pesa cada una sobre el total de la lista.
     */
    private List<PanelTendencias.Rubro> rubros(List<Object[]> filas) {
        BigDecimal total = filas.stream()
                .map(f -> cero(f[1]))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return filas.stream()
                .map(f -> {
                    BigDecimal valor = cero(f[1]);
                    return new PanelTendencias.Rubro(
                            f[0] == null ? "Sin categoría" : f[0].toString(),
                            valor,
                            participacion(valor, total));
                })
                .toList();
    }

    private static int participacion(BigDecimal valor, BigDecimal total) {
        if (total.signum() == 0 || valor.signum() <= 0) return 0;
        return valor.multiply(CIEN).divide(total, 0, RoundingMode.HALF_UP).intValue();
    }

    /** SUM() devuelve null cuando no hay filas que sumar. */
    private static BigDecimal cero(Object valor) {
        return valor == null ? BigDecimal.ZERO : (BigDecimal) valor;
    }
}
