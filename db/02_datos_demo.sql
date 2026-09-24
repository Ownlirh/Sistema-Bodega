-- ============================================================================
-- DATOS DE MOVIMIENTO PARA DEMOSTRACION
--
-- Genera ventas y compras de julio a septiembre de 2026, repartidas entre las
-- tres tiendas, para que el panel de tendencias y los reportes tengan algo que
-- mostrar sin esperar a que esten listas las pantallas de registro.
--
-- Se ejecuta DESPUES de 01_script_bd.sql.
-- Se puede volver a correr cuantas veces se quiera: borra los movimientos
-- anteriores antes de generar los nuevos. No toca productos, usuarios ni
-- tiendas.
--
-- Al final recalcula tb_stock como (todo lo comprado - todo lo vendido), asi
-- el inventario queda coherente con el historial y no inventado.
--
-- Nota tecnica: las cantidades se calculan con RAND() en el INSERT y el
-- subtotal se completa despues con un UPDATE. Si se pusiera RAND() dos veces
-- en la misma fila, MySQL lo evalua por separado y el subtotal no cuadraria
-- con la cantidad.
-- ============================================================================

USE bodegadb;

-- ------------------------------------------------- limpiar lo anterior -----
DELETE FROM tb_venta_detalle;
DELETE FROM tb_ventas;
DELETE FROM tb_compra_detalle;
DELETE FROM tb_compras;
ALTER TABLE tb_ventas  AUTO_INCREMENT = 1;
ALTER TABLE tb_compras AUTO_INCREMENT = 1;

-- ------------------------------------------------------- calendario --------
-- Fechas armadas con tablas derivadas de digitos. No se usa tabla temporal
-- porque MySQL no deja abrir la misma tabla temporal dos veces en una consulta.
DROP TEMPORARY TABLE IF EXISTS tmp_fechas;
CREATE TEMPORARY TABLE tmp_fechas (f DATE);

INSERT INTO tmp_fechas
SELECT DATE_ADD('2026-07-01', INTERVAL (a.n + b.n * 10) DAY)
FROM (SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3
      UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7
      UNION ALL SELECT 8 UNION ALL SELECT 9) a
CROSS JOIN
     (SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3
      UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7
      UNION ALL SELECT 8 UNION ALL SELECT 9) b
WHERE DATE_ADD('2026-07-01', INTERVAL (a.n + b.n * 10) DAY) <= '2026-09-20';

-- ============================================================================
-- COMPRAS A PROVEEDORES  (los egresos de mercaderia)
-- Cada tienda se abastece los dias 5 y 20 de cada mes.
-- ============================================================================

INSERT INTO tb_compras (fecha, id_tienda, id_proveedor, id_usuario, total, estado)
SELECT TIMESTAMP(f.f, '09:30:00'),
       t.id_tienda,
       pr.id_proveedor,
       (SELECT u.id_usuario FROM tb_usuarios u
         WHERE u.id_tienda = t.id_tienda AND u.id_rol = 2 LIMIT 1),
       0, 1
FROM tmp_fechas f
CROSS JOIN tb_tiendas t
CROSS JOIN tb_proveedores pr
WHERE DAY(f.f) IN (5, 20);

-- Una linea por cada producto de ese proveedor, con ~70% de probabilidad.
INSERT INTO tb_compra_detalle (id_compra, id_producto, cantidad, precio_unitario, subtotal)
SELECT c.id_compra,
       p.id_producto,
       65 + FLOOR(RAND() * 31),
       p.precio_compra,
       0
FROM tb_compras c
JOIN tb_productos p ON p.id_proveedor = c.id_proveedor
WHERE RAND() < 0.7;

UPDATE tb_compra_detalle SET subtotal = cantidad * precio_unitario;

-- Si alguna compra quedo sin lineas, se elimina: una compra vacia no existe.
DELETE FROM tb_compras
WHERE id_compra NOT IN (SELECT id_compra FROM (SELECT DISTINCT id_compra FROM tb_compra_detalle) x);

UPDATE tb_compras c
SET c.total = (SELECT COALESCE(SUM(cd.subtotal), 0)
               FROM tb_compra_detalle cd WHERE cd.id_compra = c.id_compra);

-- ============================================================================
-- VENTAS  (los ingresos)
-- Hasta 10 ventas por tienda por dia. Los fines de semana se vende mas.
-- ============================================================================

INSERT INTO tb_ventas (fecha, id_tienda, id_usuario, total, estado)
SELECT TIMESTAMP(f.f, SEC_TO_TIME(FLOOR(28800 + RAND() * 43200))),
       t.id_tienda,
       (SELECT u.id_usuario FROM tb_usuarios u
         WHERE u.id_tienda = t.id_tienda AND u.id_rol = 3
         ORDER BY RAND() LIMIT 1),
       0, 1
FROM tmp_fechas f
CROSS JOIN tb_tiendas t
CROSS JOIN (SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
            UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8
            UNION ALL SELECT 9 UNION ALL SELECT 10) veces
WHERE RAND() < (CASE WHEN DAYOFWEEK(f.f) IN (1, 7) THEN 0.75 ELSE 0.50 END);

-- Alrededor de 4 productos por venta.
INSERT INTO tb_venta_detalle (id_venta, id_producto, cantidad, precio_unitario, subtotal)
SELECT v.id_venta,
       p.id_producto,
       1 + FLOOR(RAND() * 6),
       p.precio_venta,
       0
FROM tb_ventas v
CROSS JOIN tb_productos p
WHERE p.estado = 1 AND RAND() < 0.18;

UPDATE tb_venta_detalle SET subtotal = cantidad * precio_unitario;

-- Ventas sin lineas: fuera.
DELETE FROM tb_ventas
WHERE id_venta NOT IN (SELECT id_venta FROM (SELECT DISTINCT id_venta FROM tb_venta_detalle) x);

UPDATE tb_ventas v
SET v.total = (SELECT COALESCE(SUM(vd.subtotal), 0)
               FROM tb_venta_detalle vd WHERE vd.id_venta = v.id_venta);

-- ============================================================================
-- STOCK COHERENTE
--
-- cantidad = inventario inicial + lo comprado - lo vendido, en esa tienda.
--
-- El inventario inicial (150) representa lo que la bodega ya tenia antes de
-- julio. Sin el, el stock quedaria en cero: en un negocio real se compra
-- aproximadamente lo mismo que se vende, y lo que sostiene el inventario es
-- lo que ya habia en el almacen.
-- ============================================================================

UPDATE tb_stock s
SET s.cantidad = GREATEST(0, 150
    + COALESCE((SELECT SUM(cd.cantidad)
                  FROM tb_compra_detalle cd
                  JOIN tb_compras c ON c.id_compra = cd.id_compra
                 WHERE cd.id_producto = s.id_producto
                   AND c.id_tienda   = s.id_tienda), 0)
    - COALESCE((SELECT SUM(vd.cantidad)
                  FROM tb_venta_detalle vd
                  JOIN tb_ventas v ON v.id_venta = vd.id_venta
                 WHERE vd.id_producto = s.id_producto
                   AND v.id_tienda   = s.id_tienda), 0));

-- Un par de productos bajo el minimo, para que la consulta de stock bajo
-- y la alerta tengan casos reales que mostrar.
UPDATE tb_stock SET cantidad = 2 WHERE id_producto = 1  AND id_tienda = 1;
UPDATE tb_stock SET cantidad = 0 WHERE id_producto = 9  AND id_tienda = 2;
UPDATE tb_stock SET cantidad = 3 WHERE id_producto = 13 AND id_tienda = 3;

-- ============================================================================
-- RESUMEN
-- ============================================================================

SELECT 'compras'               AS concepto, COUNT(*) AS cantidad FROM tb_compras
UNION ALL SELECT 'lineas de compra', COUNT(*) FROM tb_compra_detalle
UNION ALL SELECT 'ventas',           COUNT(*) FROM tb_ventas
UNION ALL SELECT 'lineas de venta',  COUNT(*) FROM tb_venta_detalle
UNION ALL SELECT 'productos bajo minimo',
       (SELECT COUNT(*) FROM tb_stock s
          JOIN tb_productos p ON p.id_producto = s.id_producto
         WHERE s.cantidad <= p.stock_minimo);

SELECT DATE_FORMAT(v.fecha, '%Y-%m') AS mes,
       COUNT(*)                      AS ventas,
       ROUND(SUM(v.total), 2)        AS ingresos
FROM tb_ventas v GROUP BY mes ORDER BY mes;
