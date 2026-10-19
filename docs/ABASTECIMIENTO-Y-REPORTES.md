# Abastecimiento y reportes

Módulo del integrante 6. Cubre la entrada de mercadería, los gastos
operativos, la trazabilidad por lotes y los reportes.

| Ruta | Pantalla |
|---|---|
| `/compras` | Registro de compras a proveedores |
| `/gastos` | Gastos operativos |
| `/vencimientos` | Asignación de lotes y alertas de vencimiento |
| `/mermas` | Registro de pérdidas |
| `/reportes` | Reporte de ventas, con descarga en PDF |
| `/reportes/rentabilidad` | Rentabilidad por producto |

---

## Compras

Una compra selecciona proveedor y tienda activos y admite hasta 100 productos
distintos, cada uno con su cantidad y costo unitario. **Los importes
definitivos se calculan en el servidor**, nunca se confía en lo que llega del
formulario.

La transacción guarda cabecera y detalles, aumenta el stock y escribe una
entrada `COMPRA` en el kardex por cada línea. Si algo falla, se revierte todo.

Reglas que aplica el servicio:

- Cada producto puede aparecer una sola vez en la misma compra.
- El total no puede superar `99 999 999.99`.
- El usuario debe pertenecer a la tienda, salvo que no tenga ninguna asignada
  (el administrador).
- Si la tienda nunca tuvo el producto, se crea su fila de stock en cero.

Se guarda el costo de cada detalle. **No se sobrescribe el precio de compra
del catálogo**, que es otra cosa: ver la advertencia en
[rentabilidad](#rentabilidad-por-producto).

---

## Gastos operativos

Registro de tienda, categoría, fecha, importe y descripción opcional. Son
egresos que **no son mercadería**: no tocan el stock ni el kardex.

- El importe va entre `0.01` y `99 999 999.99`, con hasta dos decimales.
- La fecha no puede ser futura, según `America/Lima`.
- La tienda y el usuario deben estar activos.

Alimentan las consultas por mes y por categoría que usa el panel de
tendencias.

---

## Lotes y vencimientos

Un lote **identifica stock que ya existe**: le pone código de envase y fecha
de vencimiento. Asignar un lote **no suma stock**.

```
stock de la tienda  =  unidades con lote  +  unidades sin lote
```

De ahí sale la regla central: no se puede asignar a lotes más de lo que hay
sin asignar. Si una tienda tiene 20 unidades y 15 ya están en un lote, el
siguiente lote admite 5 como máximo.

Flujo habitual:

1. Registrar la compra, si se acaba de recibir la mercadería.
2. En `/vencimientos`, asignar producto, tienda, código, fecha y cantidad.
3. Revisar las alertas de vencidos y por vencer.
4. Dar salida a lo perdido en `/mermas`.

Se admite identificar productos ya vencidos, y también productos inactivos que
todavía tengan stock, para poder darles salida.

---

## Mermas

Una merma descuenta **el saldo del lote y el stock de la tienda a la vez**, en
una sola transacción, y escribe un movimiento `MERMA` negativo en el kardex.
Si falla, no queda nada.

| Motivo | Cuándo |
|---|---|
| `VENCIMIENTO` | Solo si la fecha del lote ya pasó |
| `DANO`, y el resto | En cualquier momento |

El vencimiento ocurre **al finalizar** la fecha indicada, en `America/Lima`.
Un lote que vence hoy todavía no está vencido.

Un lote agotado deja de aparecer en la lista de saldos, pero sigue vinculado
al historial de mermas.

---

## Reporte de ventas en PDF

Pantalla `/reportes`, descarga en `/reportes/ventas/pdf`.

```
/reportes/ventas/pdf?desde=2026-09-01&hasta=2026-09-30&idTienda=1
```

Criterios:

- Solo ventas activas: las anuladas quedan fuera.
- **Incluye el último día completo.** La consulta usa `fecha >= desde` y
  `fecha < hasta + 1 día`, no un `BETWEEN` que cortaría a medianoche.
- La tienda es opcional. Se admite una inactiva, para consultar su historia.
- La vista previa muestra las primeras 100 filas; el PDF trae todas.
- Límite de 10 000 ventas: si se supera, se pide reducir el filtro **y no se
  trunca el reporte**. Un reporte truncado en silencio es peor que ninguno.
- El total sale de los totales guardados en cada venta, con `BigDecimal`. No
  se recalcula con precios actuales.
- Sin ventas en el período, genera igual una página con fechas, tienda,
  mensaje y total cero.

La plantilla editable está en `src/main/resources/reportes/ventas.jrxml`
(JasperReports 6.21.3, A4 horizontal, fuente DejaVu Sans embebida).

El PDF **no es un comprobante fiscal** ni representa pagos recibidos: es un
listado de ventas registradas.

---

## Rentabilidad por producto

Agrupa los detalles de ventas activas por producto y calcula:

```
ganancia = ingresos - costos
margen   = ganancia / ingresos × 100
```

Ingreso cero devuelve `N/D` en vez de dividir por cero. Las pérdidas se
muestran en negativo. El margen total se calcula sobre los importes totales,
no promediando porcentajes.

> **Advertencia sobre el costo.** Se estima con el **precio de compra actual
> del catálogo**, no con el que tenía el producto el día de la venta. No
> existe costo histórico por detalle, así que cambiar el precio de compra
> modifica la rentabilidad de ventas pasadas.
>
> Para tener exactitud histórica habría que guardar el costo dentro de
> `tb_venta_detalle`, igual que ya se guarda en `tb_compra_detalle`.

No es utilidad neta: no descuenta gastos operativos, mermas ni ninguna otra
partida.

---

## Pruebas

Este módulo es el mejor cubierto del proyecto. Tiene pruebas de unidad para
cada servicio, pruebas de controlador con renderizado real de Thymeleaf, y
generación de PDF verificada con archivos reales —vacío, de una venta y de
varias páginas— que quedan en `target/qa/pdf`.

`ComprasGastosE2ETest` y `LotesMermasE2ETest` añaden la verificación contra
una base real: que el stock suba, que un fallo no deje rastro y que la merma
descuente los dos saldos.
