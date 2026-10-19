# Seguridad y control de acceso

Este documento describe el módulo de Seguridad y Control de Acceso del Sistema de Bodega.

## Componentes

- `security/SecurityConfig.java`: define las rutas públicas, la autenticación por formulario, el logout, CSRF, la protección de sesión y los permisos por rol.
- `security/UsuarioDetailsService.java`: busca usuarios por `username` y permite actualizar su contraseña tras un inicio de sesión válido.
- `security/AppUserDetails.java`: expone el usuario, el rol y la tienda asociada a la sesión.
- `security/LegacyAwarePasswordEncoder.java`: permite una migración temporal de contraseñas heredadas en texto plano a BCrypt. Las contraseñas nuevas se codifican con BCrypt.
- `service/TiendaAccessService.java`: centraliza la validación de tiendas permitidas.
- `service/AuditoriaService.java`: guarda eventos de auditoría.
- `service/CajaService.java` y `controller/CajaController.java`: gestionan apertura, movimientos, cierre y arqueo.
- `controller/SeguridadAuditoriaController.java`: presenta los últimos eventos de auditoría al administrador.

## Matriz de permisos

| Área | Administrador | Manager | Vendedor |
|---|---|---|---|
| Inicio de sesión y cierre de sesión | Sí | Sí | Sí |
| Auditoría y diagnóstico | Sí | No | No |
| Transferencias entre tiendas | Sí | No | No |
| Reportes y panel global | Sí | No | No |
| Compras, gastos, ajustes, lotes y mermas | Sí | Sí, solo su tienda | No |
| Caja diaria | Sí | Sí, solo su tienda | Sí, solo su tienda |
| Consulta de stock bajo, alertas y kardex | Todas las tiendas | Su tienda | Su tienda |
| Lectura del catálogo de productos | Sí | Sí | Sí |
| Crear/modificar productos (POST) | Sí | Sí | No |

Los permisos de la interfaz solo mejoran la experiencia. La autorización real se aplica en el servidor mediante Spring Security y validaciones de tienda.

## Filtrado por tienda

El administrador puede consultar tiendas activas. Los demás usuarios tienen una tienda asociada a su cuenta. El sistema debe validar el `idTienda` recibido por URL o formulario antes de consultar o modificar datos.

El control se aplica en caja, compras, gastos, ajustes, lotes, mermas, alertas, kardex y stock bajo. Los reportes globales están reservados al administrador hasta que sus consultas incorporen filtros obligatorios por tienda.

## Caja diaria

1. Apertura: registra tienda, usuario, fecha y monto inicial.
2. Movimientos: acepta entradas o salidas con concepto y monto positivo.
3. Cierre: calcula efectivo esperado a partir del monto inicial, ventas y movimientos.
4. Arqueo: compara el efectivo contado con el esperado.

`Diferencia = efectivo contado - efectivo esperado`. Una diferencia positiva indica sobrante y una negativa, faltante.

## Bitácora

Se registran inicios y cierres de sesión, intentos fallidos, accesos denegados, compras, gastos, ajustes, transferencias, lotes, mermas y operaciones de caja. Cada evento conserva fecha, usuario, tienda cuando se conoce, acción, detalle, dirección IP y resultado.

La tabla se crea mediante `db/06_seguridad_caja.sql`.

## Cómo probar manualmente

1. En un entorno local de desarrollo, ejecuta `scripts/setup-bd.ps1`. **Este script recrea la base `bodegadb`; no lo ejecutes si necesitas conservar datos existentes.**
2. Arranca la aplicación y abre `http://localhost:8080/login`.
3. Inicia sesión con una cuenta de prueba que exista en `tb_usuarios`.
4. Como administrador, comprueba que puedes entrar a Auditoría y Transferencias.
5. Como manager o vendedor, prueba Caja y consultas de inventario.
6. Con una cuenta de tienda, intenta cambiar el `idTienda` en las URLs de stock bajo, kardex o caja. El servidor debe rechazar el acceso a otra tienda.
7. Cierra sesión y verifica que vuelves al formulario de login.
8. Revisa los eventos en `/seguridad/auditoria` o consulta `tb_auditoria` en MySQL.

## Antes de producción

- Cambia o elimina las cuentas de demostración.
- Elimina la compatibilidad con contraseñas en texto plano una vez migradas todas las cuentas.
- Usa HTTPS y secretos de base de datos en variables de entorno.
- No publiques contraseñas, tokens ni `application-local.properties`.
- Ejecuta `mvn test` y `mvn package` antes de fusionar el cambio. Esta copia no se pudo compilar en el entorno de edición porque Maven no está instalado.
