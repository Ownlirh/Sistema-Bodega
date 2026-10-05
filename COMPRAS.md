# Compras a proveedores — alumno 6

Ruta: `/compras`. Permite seleccionar proveedor y tienda activos, agregar hasta
100 productos distintos, indicar cantidades y costos unitarios y consultar las
últimas 50 compras. Los importes definitivos se calculan en el servidor.

La transacción guarda cabecera y detalles, aumenta el stock y registra una entrada
`COMPRA` en el kardex. Un error revierte la operación completa. Usa el bloqueo de
stock existente; además bloquea los productos en orden de ID para serializar la
creación inicial de stock entre compras. Otros módulos deben respetar el bloqueo
de stock. Una carrera con otro módulo que crea la misma fila puede ser rechazada
por la restricción única: se debe reintentar, sin guardar una compra parcial.

## Requisitos e integración

- Base `bodegadb`, tablas originales y tablas del módulo de inventario/kardex.
- Para una base existente, ejecutar `db/04_inventario_sin_borrar.sql`: crea solo
  las tablas de inventario faltantes mediante `CREATE TABLE IF NOT EXISTS`.
- No ejecutar nuevamente `01_script_bd.sql` sobre una base con datos.
- `03_inventario_logistica_tarea7.sql` contiene `DROP TABLE`: no ejecutarlo sobre
  tablas con movimientos que se deban conservar.
- `application-local.properties` se conserva únicamente en la máquina local.
- Mientras no exista login, se usa el usuario activo con ID 1, siguiendo la
  integración temporal de transferencias. El alumno 3 debe reemplazarlo por el
  usuario autenticado y aplicar los permisos y filtros de lectura por tienda.
  No se debe desplegar como módulo protegido hasta completar esa integración.
- Se guarda el costo de cada detalle. No se sobrescribe el costo del catálogo ni
  se implementan todavía lotes/vencimientos, gastos o reportes.

## Verificación

`mvn test`: pruebas de servicio (cálculo, stock existente/nuevo, validación,
duplicados, límites, tienda del usuario y propagación de errores) y MVC con
renderizado real de Thymeleaf. Las pruebas usan repositorios simulados: no
demuestran rollback ni concurrencia contra MySQL y no modifican sus datos.

Prueba manual en una base de desarrollo: anotar el stock, registrar una compra de
2 unidades a S/ 3.25 y comprobar total S/ 6.50, incremento de 2 y movimiento COMPRA
con referencia al número de compra. Recargar tras guardar no reenvía el formulario.
