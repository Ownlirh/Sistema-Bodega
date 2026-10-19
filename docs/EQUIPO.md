# Reparto de módulos

Proyecto del curso Lenguaje de Programación II — Cibertec, 2026. Siete
integrantes, cinco requisitos cada uno.

| # | Integrante | Módulo |
|---|---|---|
| 1 | Fernando | Arquitectura y panel de control |
| 2 | Eduardo | Clientes y crédito |
| 3 | Miguel | Seguridad y control de acceso |
| 4 | Angie | Ventas y movimiento de inventario |
| 5 | Carlos | Catálogos y consultas |
| 6 | Jean Piero | Abastecimiento y reportes |
| 7 | Dass | Inventario y logística |

## Detalle por integrante

**1 — Arquitectura y panel de control.** Configuración del proyecto y conexión
a base de datos. Modelo de datos y datos de prueba. Diseño común de las
pantallas. Panel de tendencias con ingresos, egresos, gastos y margen. Alertas
de stock bajo, vencimientos y deudas.

**2 — Clientes y crédito.** Mantenimiento de clientes. Registro de fiado.
Registro de pagos. Estado de cuenta por cliente. Consulta de deudores.

**3 — Seguridad y control de acceso.** Inicio y cierre de sesión. Roles y
permisos. Filtrado de datos por tienda. Caja diaria con apertura, cierre y
arqueo. Bitácora de auditoría.

**4 — Ventas y movimiento de inventario.** Registrar venta. Descuento de stock
con control de concurrencia. Comprobante de venta. Devoluciones. Kardex de
movimientos.

**5 — Catálogos y consultas.** Mantenimiento de productos. Mantenimiento de
empleados y tiendas. Mantenimiento de proveedores. Consulta de ventas por
fecha y tienda. Consulta de stock bajo el mínimo.

**6 — Abastecimiento y reportes.** Registro de compras a proveedores. Registro
de gastos operativos. Vencimientos y mermas. Reporte de ventas en PDF. Reporte
de rentabilidad por producto.

**7 — Inventario y logística.** Transferencias de stock entre tiendas. Kardex
de movimientos con saldo antes y después. Ajustes de inventario por conteo
físico. Consulta de stock bajo el mínimo. Alertas de stock y de productos por
vencer.

## Dependencias entre módulos

Conviene tenerlas presentes porque condicionan el orden de trabajo.

**Crédito depende de ventas.** `Venta` no tiene relación con `Cliente`. Sin un
`id_cliente` en `tb_ventas` no hay registro de fiado, ni estado de cuenta, ni
consulta de deudores, ni alerta de deudas. Agregarlo toca el módulo de ventas,
así que debe acordarse entre los integrantes 2 y 4 antes de que ese módulo
crezca más.

**Los reportes dependen de las ventas.** Sin ventas registradas, el reporte de
rentabilidad y el panel no tienen datos que mostrar.

**Las alertas de vencimiento dependen de los lotes.** La pantalla de alertas
lee los lotes del módulo de abastecimiento.

**Hay requisitos que se superponen.** La consulta de stock bajo el mínimo
aparece en los módulos 5 y 7, y las alertas de stock aparecen en los módulos 1
y 7. Están implementadas una sola vez; conviene dejar constancia de quién las
entregó.

## Convenciones de trabajo

- La rama de integración es `Dev`. Nadie trabaja directamente sobre ella.
- Cada integrante trabaja en su propia rama y se integra mediante Pull
  Request.
- **Antes de abrir el Pull Request hay que fusionar `Dev` en la rama propia.**
  Si no, la integración continua se ejecuta sobre código desactualizado.
- Cada uno commitea con su propio usuario de Git. La evidencia de
  participación por integrante sale del historial del repositorio.
- Un cambio de esquema va primero al script SQL de `db/` y después a la
  entidad JPA.
