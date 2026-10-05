# Gastos operativos — alumno 6

Ruta `/gastos`, disponible en el menú común. Permite registrar tienda, categoría,
fecha, importe y descripción opcional (120 caracteres). Muestra los últimos 50
gastos ordenados por fecha e ID descendentes y el total de esos registros.

Se reutilizan `tb_gastos` y `tb_categorias_gasto`; no requiere migración SQL.
El importe debe estar entre S/ 0.01 y S/ 99999999.99, con hasta dos decimales.
La fecha no puede ser futura según America/Lima. El servicio valida que la tienda
y el usuario estén activos y que el usuario pertenezca a la tienda, si tiene una
asignada. Las categorías se consultan desde la base de datos.

## Integración pendiente

Igual que compras y transferencias, se usa temporalmente el usuario con ID 1.
El alumno 3 debe sustituirlo por la sesión autenticada y aplicar permisos y filtros
de lectura por tienda antes de desplegar el módulo con acceso restringido.
Los gastos quedan disponibles para las consultas de totales por mes y categoría
ya existentes en `GastoRepository`, usadas por el panel.

## Pruebas

`mvn test` ejecuta pruebas del servicio y MVC con renderizado Thymeleaf y repositorios
simulados, sin escribir gastos en MySQL. Cubren validaciones, asociaciones,
normalización del importe, historial, total mostrado, conservación del formulario
ante errores y redirección después del registro.

Para verificar manualmente en desarrollo: seleccionar tienda y categoría, registrar
un gasto de S/ 25.50 con fecha de hoy y comprobar la confirmación y el historial.
Recargar la página después de guardar no reenvía el formulario.
