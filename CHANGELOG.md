# Registro de cambios

Qué entró en cada integración a `Dev`, de lo más reciente a lo más antiguo.

---

## 19 de octubre de 2026

### PR #11 — Documentación

El `README` pasa de estar escrito para el equipo a estarlo para un lector
externo: qué es el sistema, qué hace, cómo levantarlo con Docker, cómo
desplegar la imagen contra una base externa y cómo instalarlo sin Docker.

Se retiran cinco documentos de módulo que habían quedado afirmando cosas
falsas (que no había login, que las pruebas no verificaban rollback). Se
corrige el documento de seguridad, que listaba como componente una clase que
nunca existió. Lo interno del equipo pasa a `docs/EQUIPO.md`.

Se descarta el despliegue en Azure.

### PR #10 — Pruebas de integración

105 pruebas que levantan la aplicación completa contra una base H2 en memoria,
desechable y recreada antes de cada prueba. Cubren seguridad, compras, gastos,
inventario, lotes, mermas, caja y panel.

Cierra un hueco de fondo: hasta acá las consultas JPQL de los 22 repositorios
**nunca se habían ejecutado** en ninguna prueba, porque en los `@WebMvcTest`
los repositorios están mockeados. La matriz de permisos tampoco la verificaba
nadie.

La suite pasa de 84 a 189 pruebas.

### PR #9 — Panel de tendencias

Nueva ruta `/panel`, con filtro opcional por tienda. Cruza ingresos, egresos
de mercadería y gastos operativos mes a mes y calcula el margen. Incluye
gráficos de participación por categoría de gasto y por tienda.

Se arregla además el menú: mostraba un texto fijo en vez del usuario de la
sesión, no tenía botón para cerrar sesión, y ofrecía a todos los roles enlaces
que terminaban en 403.

### PR #8 — Integración continua

Dos workflows de GitHub Actions sobre cada Pull Request: uno compila y publica
el JAR, el otro ejecuta la suite y guarda los reportes.

### PR #7 — Corrección de compilación y entorno Docker

El proyecto no compilaba: el `pom.xml` tenía las dependencias de Spring
Security dentro de un bloque comentado, mientras el código ya las usaba.
Detrás de eso había dos errores más que nunca habían llegado a manifestarse, y
19 pruebas que jamás se habían ejecutado.

Se agrega el entorno en contenedores: `docker compose up -d` levanta base y
aplicación, sin necesidad de instalar Java, Maven ni MySQL.

### PR #6 — Clientes

Mantenimiento de clientes con baja lógica. *(Eduardo)*

### PR #5 — Seguridad, caja y auditoría

Autenticación por formulario, tres roles, filtrado de datos por tienda,
migración de contraseñas a BCrypt, caja diaria con arqueo y bitácora de
auditoría. *(Miguel)*

---

## 5 de octubre de 2026

### PR #4 — Inventario y logística

Transferencias entre tiendas, kardex con saldo antes y después, ajustes por
conteo físico, consulta de stock bajo el mínimo y pantalla de alertas.
*(Dass)*

### PR #3 — Abastecimiento y reportes

Compras a proveedores, gastos operativos, lotes con vencimiento, mermas,
reporte de ventas en PDF con JasperReports y rentabilidad por producto.
*(Jean Piero)*

---

## 23 de septiembre de 2026

### Base del proyecto

Estructura inicial, entidades JPA, repositorios, scripts de base de datos con
datos de prueba, diseño común de las pantallas y scripts de instalación.
*(Fernando)*

---

## Pendiente de integrar

| Rama | Contenido |
|---|---|
| `ventas` | Registro de ventas, control de concurrencia, comprobante y devoluciones |
| `carlosavalos/catalogos-y-consultas` | Mantenimiento de productos, empleados, tiendas y proveedores |

Mientras no se integren, los enlaces del menú a esas pantallas no resuelven.
