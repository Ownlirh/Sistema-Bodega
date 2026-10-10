package pe.edu.cibertec.bodega.service;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.cibertec.bodega.dto.*;
import pe.edu.cibertec.bodega.repository.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Service @RequiredArgsConstructor
public class ReporteVentasService {
    private final VentaRepository ventas;
    private final TiendaRepository tiendas;
    private final Validator validator;
    @Transactional(readOnly = true)
    public ReporteVentas consultar(ReporteVentasForm form) {
        if (form == null || !validator.validate(form).isEmpty())
            throw new IllegalArgumentException("Ingrese ambas fechas.");
        if (form.getDesde().isAfter(form.getHasta()))
            throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la fecha final.");
        if (form.getDesde().getYear() < 1000 || form.getHasta().isAfter(LocalDate.of(9999, 12, 30)))
            throw new IllegalArgumentException("El rango de fechas está fuera del permitido.");
        String tienda = form.getIdTienda() == null ? "Todas las tiendas" : tiendas.findById(form.getIdTienda())
            .orElseThrow(() -> new IllegalArgumentException("La tienda no existe.")).getNombre();
        var lista = ventas.buscarParaReporte(form.getDesde().atStartOfDay(), form.getHasta().plusDays(1).atStartOfDay(),
            form.getIdTienda(), PageRequest.of(0, 10001));
        if (lista.size() > 10000)
            throw new IllegalArgumentException("El reporte supera 10 000 ventas. Reduzca el rango o seleccione una tienda.");
        var filas = lista.stream().map(v -> new ReporteVentas.Fila(v.getIdVenta(), v.getFecha(),
            v.getTienda().getNombre(), v.getUsuario().getNombreCompleto(), v.getTotal())).toList();
        var total = filas.stream().map(ReporteVentas.Fila::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ReporteVentas(form.getDesde(), form.getHasta(), tienda, filas, total);
    }
}
