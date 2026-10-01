package pe.edu.cibertec.bodega.service;
import com.lowagie.text.pdf.PdfReader;
import org.junit.jupiter.api.Test;
import pe.edu.cibertec.bodega.dto.ReporteVentas;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ReporteVentasPdfTest {
    private ReporteVentas reporte(int cantidad) {
        List<ReporteVentas.Fila> filas = new ArrayList<>();
        for (int i=1; i<=cantidad; i++) filas.add(new ReporteVentas.Fila(i,
            LocalDateTime.of(2026,9,15,10,30), "Bodega La Unión - Sucursal de demostración",
            "María José Fernández de la Cruz - datos de prueba", new BigDecimal("12.50")));
        return new ReporteVentas(LocalDate.of(2026,9,1),LocalDate.of(2026,9,30),"Todas las tiendas (DATOS DE PRUEBA)",filas,
            new BigDecimal("12.50").multiply(BigDecimal.valueOf(cantidad)));
    }
    private int generar(int cantidad, String nombre) throws Exception {
        byte[] bytes = new ReporteVentasPdf().generar(reporte(cantidad));
        Path carpeta = Path.of("target", "qa", "pdf"); Files.createDirectories(carpeta); Files.write(carpeta.resolve(nombre), bytes);
        assertEquals("%PDF-", new String(bytes, 0, 5, java.nio.charset.StandardCharsets.US_ASCII));
        PdfReader reader = new PdfReader(bytes);
        try { return reader.getNumberOfPages(); } finally { reader.close(); }
    }
    @Test void generaPDFVacioLegible() throws Exception { assertEquals(1, generar(0,"ventas-vacio.pdf")); }
    @Test void generaUnaVenta() throws Exception { assertEquals(1, generar(1,"ventas-una.pdf")); }
    @Test void paginaMuchasVentasSinRecortarFilas() throws Exception { assertTrue(generar(120,"ventas-multipagina.pdf") > 1); }
}
