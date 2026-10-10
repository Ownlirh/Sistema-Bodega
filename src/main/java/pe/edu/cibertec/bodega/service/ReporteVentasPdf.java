package pe.edu.cibertec.bodega.service;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import pe.edu.cibertec.bodega.dto.ReporteVentas;
import java.util.*;
import java.time.*;
import java.time.format.DateTimeFormatter;

@Service
public class ReporteVentasPdf {
    private volatile JasperReport plantilla;
    private synchronized JasperReport plantilla() throws Exception {
        if (plantilla == null) {
            try (var in = new ClassPathResource("reportes/ventas.jrxml").getInputStream()) {
                plantilla = JasperCompileManager.compileReport(in);
            }
        }
        return plantilla;
    }
    public byte[] generar(ReporteVentas reporte) {
        try {
            DateTimeFormatter fecha = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            DateTimeFormatter fechaHora = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            Map<String, Object> parametros = new HashMap<>();
            parametros.put("PERIODO", reporte.getDesde().format(fecha) + " al " + reporte.getHasta().format(fecha));
            parametros.put("TIENDA", reporte.getTienda()); parametros.put("TOTAL", reporte.getTotal());
            parametros.put("GENERADO", LocalDateTime.now(ZoneId.of("America/Lima")).format(fechaHora) + " (Lima)");
            parametros.put(JRParameter.REPORT_LOCALE, Locale.US);
            Collection<Map<String, ?>> filas = new ArrayList<>();
            for (var fila : reporte.getFilas()) {
                Map<String, Object> datos = new HashMap<>();
                datos.put("numero", fila.getNumero()); datos.put("fecha", fila.getFecha().format(fechaHora));
                datos.put("tienda", fila.getTienda()); datos.put("vendedor", fila.getVendedor());
                datos.put("total", fila.getTotal()); filas.add(datos);
            }
            return JasperExportManager.exportReportToPdf(JasperFillManager.fillReport(plantilla(), parametros,
                new JRMapCollectionDataSource(filas)));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el PDF de ventas.", e);
        }
    }
}
