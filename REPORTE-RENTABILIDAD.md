# Rentabilidad por producto

En Reportes, abrir «Ver rentabilidad por producto». Consulta por fechas inclusivas y tienda opcional. No necesita migraciones SQL.

Los ingresos proceden de los subtotales guardados en detalles de ventas activas. El costo estimado es unidades vendidas por precio de compra ACTUAL del catálogo, siguiendo el modelo existente. No existe costo histórico por detalle; cambiar el catálogo cambia esta estimación histórica. No es utilidad neta: no resta gastos, mermas ni otras partidas.

Agrupa por identidad del producto (no solamente descripción), incluye productos y tiendas históricos, excluye ventas anuladas y ordena por ganancia descendente. Margen = (ingresos - costos) / ingresos × 100, redondeado a dos decimales. Ingreso cero produce N/D; las pérdidas se muestran negativas. El margen total usa sumas, no promedio de porcentajes.

La autorización y restricción de tiendas por usuario deben integrarse con el módulo de seguridad del alumno 3. La descarga PDF corresponde al reporte de ventas; rentabilidad se consulta en pantalla.
