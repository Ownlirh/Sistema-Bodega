# Seguridad y control de acceso

Módulo del alumno 3. Este documento se conserva porque la matriz de permisos
es la referencia del equipo: si alguien cambia una regla en `SecurityConfig`,
tiene que cambiarla también acá y en el navbar.

## Componentes

| Archivo | Qué hace |
|---|---|
| `security/SecurityConfig.java` | Rutas públicas, login por formulario, logout, CSRF, protección de sesión y permisos por rol |
| `security/UsuarioDetailsService.java` | Busca usuarios por `username` y actualiza su clave tras un login válido |
| `security/AppUserDetails.java` | Expone usuario, rol y tienda de la sesión |
| `security/LegacyAwarePasswordEncoder.java` | Migra claves heredadas en texto plano a BCrypt. Las nuevas siempre van con BCrypt |
| `security/SesionModelAdvice.java` | Deja `usuarioSesion`, `rolSesion` y `tiendaSesion` en el modelo de todas las pantallas |
| `service/TiendaAccessService.java` | Centraliza la validación de tiendas permitidas |
| `service/AuditoriaService.java` | Guarda eventos en la bitácora |
| `service/CajaService.java` + `controller/CajaController.java` | Apertura, movimientos, cierre y arqueo |

## Matriz de permisos

Esta matriz la verifica `SeguridadE2ETest`, ruta por ruta y rol por rol.

| Área | Administrador | Manager | Vendedor |
|---|---|---|---|
| Inicio y cierre de sesión | Sí | Sí | Sí |
| Auditoría y diagnóstico (`/health/**`) | Sí | No | No |
| Transferencias entre tiendas | Sí | No | No |
| Reportes y panel global | Sí | No | No |
| Compras, gastos, ajustes, lotes y mermas | Sí | Sí, solo su tienda | No |
| Caja diaria | Sí | Sí, solo su tienda | Sí, solo su tienda |
| Stock bajo, alertas y kardex | Todas las tiendas | Su tienda | Su tienda |
| Lectura del catálogo de productos | Sí | Sí | Sí |
| Crear o modificar productos (POST) | Sí | Sí | No |

Esconder un enlace en el navbar **no es la protección**: eso solo evita
ofrecer una puerta que va a dar 403. La autorización real la aplica
`SecurityConfig` en el servidor.

## Filtrado por tienda

El administrador no tiene tienda asignada y ve todas. Los demás usuarios
tienen una, y el sistema valida el `idTienda` que llega por URL o formulario
antes de consultar o modificar nada.

Se aplica en caja, compras, gastos, ajustes, lotes, mermas, alertas, kardex y
stock bajo. Los reportes globales quedan reservados al administrador hasta que
sus consultas incorporen un filtro obligatorio por tienda.

## Caja diaria

1. **Apertura:** registra tienda, usuario, fecha y monto inicial. Solo puede
   haber una caja abierta por tienda a la vez.
2. **Movimientos:** entradas o salidas, con concepto y monto positivo.
3. **Cierre y arqueo:**

```
esperado   = monto inicial + ventas del turno + entradas - salidas
diferencia = efectivo contado - esperado
```

Una diferencia positiva es sobrante; una negativa, faltante. Las ventas
anuladas y las de otras tiendas no entran en el cálculo.

## Bitácora

Se registran inicios y cierres de sesión, intentos fallidos, accesos denegados
y las operaciones de caja. Cada evento guarda fecha, usuario, tienda cuando se
conoce, acción, detalle, dirección IP y resultado.

La tabla sale de `db/06_seguridad_caja.sql`.

## Pendientes conocidos del módulo

Estos puntos están verificados contra la aplicación corriendo:

- **`/caja` devuelve 500.** `CajaController` devuelve la vista `caja/index` y
  esa plantilla no existe. Toda la lógica de apertura, movimientos y arqueo
  está escrita y probada, pero no se puede usar desde el navegador.
- **`/seguridad/auditoria` devuelve 404.** La plantilla
  `templates/seguridad/auditoria.html` existe y `SecurityConfig` ya protege la
  ruta, pero **falta el controlador que la sirva**. La bitácora se escribe
  bien; no hay forma de leerla desde la aplicación.
- **El id de usuario sigue hardcodeado en `1`** en `CompraController:39`,
  `VencimientoController:36` y `VencimientoController:67`, con el comentario
  "Sustituir por usuario autenticado". Hay que tomarlo con
  `@AuthenticationPrincipal AppUserDetails`.

## Cómo probarlo

La suite lo cubre de punta a punta, contra una base de verdad:

```bash
docker compose --profile dev run --rm --no-deps app-dev mvn -B test
```

`SeguridadE2ETest` verifica login, logout con sesión invalidada, la migración
a BCrypt, la matriz de roles completa y los tres tipos de evento de bitácora.
`CajaE2ETest` cubre el ciclo de caja y la cuenta del arqueo.

Para probarlo a mano, con la aplicación en `http://localhost:8080`: entrá con
cuentas de distinto rol y comprobá que el navbar cambia, que las rutas
prohibidas llevan a `/403`, y que cambiar el `idTienda` en la URL de stock
bajo o kardex es rechazado.

## Antes de la entrega

- Cambiar o eliminar las cuentas de demostración.
- Quitar la compatibilidad con claves en texto plano una vez migradas todas.
- No publicar contraseñas, tokens ni `application-local.properties`.
