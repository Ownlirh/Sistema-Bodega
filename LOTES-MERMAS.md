# Lotes, vencimientos y mermas — alumno 6

Ejecutar una vez `db/05_lotes_mermas.sql` sobre `bodegadb` (requiere las tablas
base y kardex). Usa `CREATE TABLE IF NOT EXISTS` y no cambia stock ni registros
existentes. La aplicación mantiene `ddl-auto=validate`.

## Uso

1. Registrar la compra si se acaba de recibir mercadería.
2. En `/vencimientos`, asignar al stock existente producto, tienda, código del
   envase, fecha y cantidad. Esto **no suma stock**. La cantidad no puede exceder
   el stock que aún no está asignado a lotes. Se admite identificar productos ya
   vencidos y productos inactivos que todavía tengan stock para darles salida.
3. Revisar alertas de lotes vencidos y por vencer entre hoy y los próximos 30 días.
   El vencimiento ocurre al finalizar la fecha indicada, en America/Lima.
4. En `/mermas`, seleccionar un lote y registrar cantidad, motivo y descripción.
   Solo se acepta VENCIMIENTO si la fecha ya pasó. Para daño, rotura u otra causa
   puede retirarse mercadería antes del vencimiento.

Una transacción descuenta saldo del lote y stock, guarda la merma y agrega un
movimiento `MERMA` negativo al kardex. Si falla, todo se revierte. El vencimiento
por sí solo nunca descuenta stock. Un lote agotado deja de aparecer en la lista
de saldos, pero permanece vinculado al historial de mermas.

## Integración con el equipo

- Se reutiliza el bloqueo de stock y se bloquea/refresca el lote antes de la merma.
- `TransferenciaService` llama a `LoteService.trasladar` dentro de su transacción.
  Se trasladan primero los lotes con vencimiento más cercano, conservando código
  y fecha, y luego las unidades sin lote. La operación no suma stock adicional.
  El operador debe mover físicamente esos lotes. No se fusionan lotes de distintas
  entradas aunque compartan código, para conservar las cantidades de cada entrada.
- No hay servicio de ventas/devoluciones/ajustes implementado en esta rama. Al
  integrarlos, sus responsables deben descontar/reponer los lotes además del stock;
  usar orden de bloqueo stock→lotes y evitar vender unidades vencidas. No interpretar
  esta pantalla como bloqueo automático de ventas todavía inexistentes.
- El usuario sigue siendo temporal (ID 1), igual que los módulos existentes.
  Falta integrar sesión, permisos y lectura limitada por tienda con el alumno 3.
- El costo y la compra de origen no se asignan a los lotes en este módulo: son
  identificación del stock existente. No utilizar cantidadInicial como compras o
  ingresos, pues también se crean filas al transferir.

## Pruebas

`mvn test`: asignación sin duplicar stock, límites, retiro por daño/vencimiento,
stock/lote insuficiente, usuario de otra tienda, propagación de errores del kardex,
conservación de datos en transferencias y renderizado/validación de ambas pantallas.
Las pruebas unitarias usan repositorios simulados; no prueban carreras concurrentes
ni rollback de MySQL. Validar esas condiciones antes de integrar con las ventas.
