# Reporte de ventas en PDF — alumno 6

Pantalla `/reportes`. Descarga `/reportes/ventas/pdf?desde=2026-09-01&hasta=2026-09-30`
con `idTienda` opcional. Requiere actualizar el proyecto Maven en Eclipse para
cargar JasperReports y sus fuentes, incorporados al `pom.xml`.

## Criterios

- Solo ventas activas (`estado = true`); las anuladas quedan excluidas.
- Incluye todo el último día: consulta `fecha >= desde` y `fecha < hasta + 1 día`.
- Tienda opcional: todas o una existente, incluso inactiva para consultar su historia.
- Vista previa de las primeras 100 filas; el PDF incluye todos los resultados.
- Límite de 10 000 ventas: si se excede, se pide reducir el filtro y no se trunca el reporte.
- Total en soles calculado con BigDecimal desde los totales de las ventas, sin recalcular
  con precios actuales. No representa ganancia, pagos recibidos ni un comprobante fiscal.
- Sin ventas, genera una página con fechas, tienda, mensaje y total cero.
- La descarga vuelve a consultar la base: refleja el estado de los datos al descargar.

## Diseño e integración

Plantilla editable `src/main/resources/reportes/ventas.jrxml`, JasperReports 6.21.3,
fuente DejaVu Sans embebida, A4 horizontal y columnas de número, fecha, tienda,
vendedor y total. Encabezados se repiten; incluye fecha de generación (Lima), número
de página, cantidad de ventas y total al final.

No requiere cambios de base. No inserta ventas ni ejecuta los datos de demostración.
El módulo de ventas del alumno 4 deberá registrar los datos que alimentan este reporte.
El alumno 3 debe añadir autorización y limitar la selección/consulta por la tienda
del usuario autenticado; la base actual todavía no tiene login.

## Verificación

`mvn test` incluye validación de rangos, límites, totalización, filtros, descarga HTTP
y PDFs reales vacíos, de una venta y de varias páginas. Los PDFs de prueba se guardan
en `target/qa/pdf` con datos ficticios y no modifican MySQL.
