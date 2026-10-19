# Inventario y logística

Módulo del integrante 7. Cubre el movimiento de stock entre tiendas, los
ajustes por conteo físico, el historial de movimientos y las alertas.

| Ruta | Pantalla |
|---|---|
| `/transferencias` | Traslado de stock entre tiendas |
| `/ajustes` | Ajuste por conteo físico |
| `/kardex` | Historial de movimientos |
| `/inventario/stock-bajo` | Productos por debajo del mínimo |
| `/alertas` | Agotados, bajo mínimo, vencidos y por vencer |

---

## Kardex

Es el historial único del inventario. Cada fila guarda el saldo **antes** y
**después** del movimiento, de modo que se puede reconstruir cómo llegó el
stock a su valor actual sin recalcular nada.

| Campo | Significado |
|---|---|
| `tipo` | `COMPRA`, `MERMA`, `AJUSTE`, `TRANSFERENCIA` |
| `cantidad` | Positiva si entra, negativa si sale |
| `stockAntes` | Saldo previo |
| `stockDespues` | `stockAntes + cantidad` |
| `referencia` | Documento que lo originó, por ejemplo `Compra #12` |

`KardexService.registrar` es el único punto de escritura. **Cualquier
operación nueva que mueva stock debe llamarlo**, o el historial queda con
huecos y deja de cuadrar.

---

## Transferencias entre tiendas

Mueven stock de una tienda a otra dentro de una sola transacción. El total del
sistema no cambia: lo que sale de origen entra en destino.

Qué hace la operación:

1. Valida que la cantidad sea positiva y que origen y destino sean distintos.
2. Bloquea el stock de origen y comprueba que alcance.
3. Bloquea el stock de destino, creando la fila en cero si esa tienda nunca
   tuvo el producto.
4. Resta en origen y suma en destino.
5. Traslada los lotes con `LoteService.trasladar`, dentro de la misma
   transacción.
6. Guarda la transferencia y escribe **dos** movimientos de kardex: una salida
   en origen y una entrada en destino.

Si cualquier paso falla, no queda rastro: ni transferencia, ni kardex, ni
cambio de stock.

> **Riesgo conocido:** dos transferencias opuestas y simultáneas del mismo
> producto pueden provocar un interbloqueo. Está explicado, con su solución,
> en [ARQUITECTURA.md](ARQUITECTURA.md#riesgo-conocido-transferencias-cruzadas).

### Qué pasa con los lotes

Viajan con la mercadería. Se trasladan primero los de vencimiento más cercano,
conservando código y fecha, y se crea un lote nuevo en destino por cada uno
movido. Las unidades que no estaban asignadas a ningún lote se mueven igual,
pero siguen sin lote en destino.

No se fusionan lotes de distintas entradas aunque compartan código: cada
entrada conserva su cantidad.

El operador tiene que mover físicamente esos lotes. El sistema registra la
intención, no la ejecuta.

---

## Ajustes por conteo físico

Cuando lo contado no coincide con lo que dice el sistema, el ajuste deja el
stock **igual a lo contado** y registra la diferencia.

```
diferencia = stock físico - stock del sistema
```

| Caso | Resultado |
|---|---|
| Contado menor | Diferencia negativa, el stock baja |
| Contado mayor | Diferencia positiva, el stock sube |
| Contado igual | Se rechaza: no hay nada que ajustar |

El kardex guarda **la diferencia**, no el total contado. Si había 20 y se
cuentan 17, el movimiento es de `-3`, no de `17`.

Se rechazan los conteos negativos y los ajustes sin motivo: un ajuste sin
explicación es un agujero en la trazabilidad.

---

## Alertas y stock bajo

`/inventario/stock-bajo` lista los productos de una tienda cuyo stock está en
o por debajo de su `stockMinimo`.

`/alertas` junta cuatro situaciones en una sola pantalla:

| Alerta | Criterio |
|---|---|
| Agotados | Stock en 0 |
| Bajo el mínimo | Stock mayor que 0 y menor o igual al mínimo |
| Vencidos | Lotes con saldo y fecha de vencimiento ya pasada |
| Por vencer | Lotes con saldo que vencen en los próximos 30 días |

Las dos últimas dependen de los lotes del módulo de abastecimiento. El umbral
de treinta días está en la constante `DIAS_AVISO` de `AlertaController`.

**El vencimiento no descuenta stock por sí solo.** Marca el lote para que
alguien decida; la salida se registra como merma.

---

## Pruebas

`InventarioE2ETest` cubre este módulo contra una base real: que el total se
conserve al transferir, que un fallo no deje rastro, que no se pueda vaciar
más de lo que hay, que el kardex guarde el antes y el después, y que un ajuste
pueda disparar o apagar una alerta de stock bajo.
