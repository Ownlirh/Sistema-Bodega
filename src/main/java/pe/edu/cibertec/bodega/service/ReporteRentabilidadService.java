package pe.edu.cibertec.bodega.service;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.cibertec.bodega.dto.*;
import pe.edu.cibertec.bodega.repository.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Service @RequiredArgsConstructor
public class ReporteRentabilidadService {
    private final VentaDetalleRepository detalles;
    private final TiendaRepository tiendas;
    private final Validator validator;
    @Transactional(readOnly = true)
    public ReporteRentabilidad consultar(ReporteVentasForm filtro) {
        if (filtro == null || !validator.validate(filtro).isEmpty())
            throw new IllegalArgumentException("Ingrese ambas fechas.");
        if (filtro.getDesde().isAfter(filtro.getHasta()))
            throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la fecha final.");
        if (filtro.getDesde().getYear() < 1000 || filtro.getHasta().isAfter(LocalDate.of(9999,12,30)))
            throw new IllegalArgumentException("El rango de fechas está fuera del permitido.");
        String tienda = filtro.getIdTienda() == null ? "Todas las tiendas" : tiendas.findById(filtro.getIdTienda())
            .orElseThrow(() -> new IllegalArgumentException("La tienda no existe.")).getNombre();
        var filas = detalles.buscarRentabilidad(filtro.getDesde().atStartOfDay(), filtro.getHasta().plusDays(1).atStartOfDay(), filtro.getIdTienda())
            .stream().map(r -> new ReporteRentabilidad.Fila((Integer) r[0], (String) r[1], (String) r[2],
                ((Number) r[3]).longValue(), (BigDecimal) r[4], (BigDecimal) r[5])).toList();
        return new ReporteRentabilidad(tienda, filas,
            filas.stream().map(ReporteRentabilidad.Fila::getIngresos).reduce(BigDecimal.ZERO, BigDecimal::add),
            filas.stream().map(ReporteRentabilidad.Fila::getCostos).reduce(BigDecimal.ZERO, BigDecimal::add));
    }
}
