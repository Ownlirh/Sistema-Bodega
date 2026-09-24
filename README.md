# Sistema de ventas para bodegas

Proyecto del curso **Lenguaje de Programación II** — Cibertec, 2026.

Esta es la **base del proyecto**: base de datos, conexión, entidades JPA y
repositorios. Está pensada para que cada integrante construya su parte encima
sin pisarse con los demás.

> **Estado: probado.** Compila (`BUILD SUCCESS`, 28 clases), la base carga y la
> aplicación levanta conectada a MySQL. Verificado el 21/09/2026 en Windows 11
> con JDK 17.0.20, Maven 3.9.9 y MySQL 8.4.9.

---

## 1. Arranque rápido (Windows)

Cuatro comandos, en orden, desde PowerShell parado en la carpeta del proyecto.

**Paso 1 — revisa qué te falta.** No instala nada, solo mira:

```bash
.\scripts\verificar.ps1
```

**Paso 2 — instala lo que falte.** Usa winget, así que no hay que buscar
instaladores:

```bash
.\scripts\instalar.ps1
```

> Al terminar, **cierra la terminal y abre una nueva**. Windows no refresca el
> PATH en ventanas que ya estaban abiertas.
>
> El instalador de MySQL abre su propia ventana y te pide definir la clave de
> root. **Anótala**, la necesitas en el paso siguiente.

**Paso 3 — arranca MySQL:**

```bash
.\scripts\mysql-iniciar.ps1
```

**Paso 4 — crea la base de datos** con sus datos de prueba:

```bash
.\scripts\setup-bd.ps1
```

**Paso 5 — levanta la aplicación:**

```bash
.\scripts\iniciar.ps1
```

Cuando veas `Started BodegaApplication`, abre <http://localhost:8080/health/db>.
Si te devuelve los conteos de cada tabla, tu entorno está listo.

Del día siguiente en adelante solo necesitas los pasos 3 y 5.

### Qué se instala

| Herramienta | Para qué | Cómo |
|---|---|---|
| JDK 17 | Compilar y ejecutar. Spring Boot 3 no corre con Java 11 | winget `EclipseAdoptium.Temurin.17.JDK` |
| Maven 3.9.9 | Dependencias y build | Zip de Apache, **no está en winget** |
| MySQL 8.4 | La base de datos | winget `Oracle.MySQL` |
| Git | Control de versiones | winget `Git.Git` |

### Dos cosas raras del entorno que conviene saber

**MySQL corre como proceso, no como servicio.** El instalador silencioso solo
copia archivos: no inicializa el servidor ni registra el servicio de Windows,
porque eso pide permisos de administrador. Por eso existe
`mysql-iniciar.ps1`, y por eso **hay que correrlo cada vez que reinicias la
PC**. Si prefieres que arranque solo, al final de ese script están las dos
líneas para registrarlo como servicio (necesitas PowerShell como
administrador).

**El usuario `root` local no tiene clave.** Es lo normal en un MySQL recién
inicializado para desarrollo y es lo que espera la configuración por defecto.

Si el tuyo sí tiene clave, **no edites `application.properties`**: ese archivo
es de todo el equipo. Copia la plantilla y pon ahí tus datos:

```bash
Copy-Item application-local.properties.ejemplo application-local.properties
```

Descomenta las líneas que necesites y listo. Ese archivo está en `.gitignore`,
así que es solo tuyo y nadie va a pisar tu configuración.

Para los scripts, que van por fuera de Spring:

```bash
.\scripts\setup-bd.ps1 -ConClave
```

```bash
.\scripts\iniciar.ps1 -Clave tu_clave
```

### Cómo se resuelven las credenciales

De mayor a menor prioridad:

| Orden | Origen | Para qué |
|---|---|---|
| 1 | `application-local.properties` | Tu configuración personal. No se comparte |
| 2 | Variables de entorno `DB_USER` / `DB_PASSWORD` | Útil si prefieres no crear archivos |
| 3 | `application.properties` | Los valores por defecto del equipo |

Comprobado: con un `application-local.properties` que tenía una clave
incorrecta, la aplicación falló con `Access denied`, lo que confirma que el
archivo se lee y pisa lo demás. Al borrarlo, volvió a arrancar con los valores
por defecto.

Si usas VS Code, el script también le agrega las extensiones de Java y Spring
Boot. Si prefieres IntelliJ IDEA Community:

```bash
winget install --id JetBrains.IntelliJIDEA.Community -e
```

### Alternativa: MySQL en Docker

Si el instalador de MySQL te complica, hay un `docker-compose.yml` que levanta
MySQL con la base ya cargada. Necesitas Docker Desktop, pero te ahorra
configurar el servicio:

```bash
docker compose up -d
```

Con eso te saltas el paso 3. La clave queda en `root` / `root`, que es lo que
espera la configuración por defecto.

## 2. Si algo falla

| Síntoma | Causa casi siempre |
|---|---|
| `java` o `mvn` no se reconoce | No abriste una terminal nueva después de instalar |
| `Access denied for user 'root'@'localhost' (using password: YES)` | Le estás mandando clave a un root que no tiene. Usa `.\scripts\iniciar.ps1` sin `-Clave` |
| `Communications link failure` | MySQL está apagado. `.\scripts\mysql-iniciar.ps1` |
| `Unknown database 'bodegadb'` | Falta el paso 4 |
| `Schema-validation: wrong column type` | Una entidad no coincide con su tabla. El mensaje dice exactamente qué columna. Arregla el script SQL **y** la entidad, y recarga con `setup-bd.ps1` |
| `Unable to determine Dialect without JDBC metadata` | No es un problema de dialecto: es que no logró conectarse. Busca el error real más arriba en el log |
| El puerto 8080 está ocupado | Cambia `server.port` en `application.properties` |

## 3. Comprobar que quedó bien

Dos rutas temporales para verificar el entorno sin depender del trabajo de los
demás:

- <http://localhost:8080/health/db> — conteo de filas por tabla
- <http://localhost:8080/health/relaciones> — prueba que el mapeo entre
  entidades funciona

**Hay que borrar `HealthController` antes de la entrega final.** No es parte
del sistema.

---

## 4. Cómo está organizado

```
src/main/java/pe/edu/cibertec/bodega/
├── model/        13 entidades JPA               ← LISTO
├── repository/   13 repositorios                ← LISTO
├── controller/   Home + Health + DemoProducto   ← base y ejemplos
├── service/      vacío                          ← integrantes 3 y 4
└── config/       vacío                          ← integrante 3

src/main/resources/
├── application.properties                       ← LISTO
├── static/css/estilos.css                       ← LISTO
└── templates/
    ├── fragments/comunes.html                   ← LISTO (cabecera, navbar, pie)
    ├── index.html                               ← LISTO
    └── demo/productos.html                      ← ejemplo a copiar

db/01_script_bd.sql                              ← LISTO
scripts/*.ps1                                    ← LISTO
```

### Cómo hacer una pantalla nueva

Todas las pantallas usan los mismos tres fragmentos, así salen iguales sin que
nadie tenga que ponerse de acuerdo en el diseño:

```html
<head th:replace="~{fragments/comunes :: cabecera('Mi titulo')}"></head>
<body>
<div th:replace="~{fragments/comunes :: navbar}"></div>
<main class="contenedor">
    <!-- tu contenido -->
</main>
<div th:replace="~{fragments/comunes :: pie}"></div>
</body>
```

**Copien `templates/demo/productos.html` y `DemoProductoController.java`**: entre
los dos muestran el camino completo de controlador a vista, con tabla, formato
de decimales, etiquetas de estado y el caso de lista vacía.

Las clases de CSS que ya existen: `panel`, `table.datos`, `boton`, `campo`,
`tarjeta`, `etiqueta ok|alerta|error`, `aviso ok|alerta|error`. Si les falta
algo, agréguenlo al final de `estilos.css`, en la sección COMPLEMENTOS.

Si agregan un módulo, sumen su enlace al navbar en `fragments/comunes.html`.
Las rutas ya están puestas ahí y apuntan a pantallas que todavía no existen
(dan 404 hasta que las creen).

### El modelo de datos

```
Rol ──< Usuario >── Tienda
                      │
Categoria ──< Producto >── Proveedor
                 │
                 └──< Stock >── Tienda     (inventario POR tienda)

Venta ──< VentaDetalle >── Producto        (ingresos)
Compra ──< CompraDetalle >── Producto      (egresos de mercadería)
Gasto ── CategoriaGasto                    (egresos operativos)
```

Tres decisiones que conviene entender antes de tocar nada:

**El stock es por tienda, no global.** `tb_stock` cruza producto × tienda. Una
bodega puede tener 20 unidades de algo y otra ninguna.

**`Producto` guarda precio de compra y precio de venta.** De su diferencia sale
el margen que alimenta el reporte de rentabilidad y el panel. No quitar ninguno
de los dos.

**`Stock` tiene un campo `version` con `@Version`.** Es el bloqueo optimista de
JPA y es lo que impide que dos cajas vendan la misma última unidad. No borrarlo.

---

## 5. Qué le toca a cada uno

Somos 6. Cada requisito del checklist tiene un dueño único.

### Integrante 1 — arquitectura e infraestructura
Ya entregó: proyecto base, entidades, repositorios, script SQL, scripts de
instalación y el layout de Thymeleaf.
Le queda: repositorio en GitHub, GitHub Actions y despliegue en Azure.

### Integrante 2 — coordinación y análisis
El informe completo, los requisitos, los casos de uso y el diagnóstico SEPTE.
También arma el video demo reel con las grabaciones de todos.
Es mucho trabajo: la plantilla tiene doce secciones.

### Integrante 3 — seguridad, roles y sesiones
Descomenta las dependencias de Spring Security en el `pom.xml` y crea la
configuración en `config/`.

**Aviso al equipo antes de subir ese commit.** Apenas entre esa dependencia,
Spring Security bloquea todas las rutas y nadie puede probar nada hasta que la
configuración esté lista. Déjalo funcionando el mismo día.

Las claves en la base están en texto plano solo para poder probar: hay que
reemplazarlas por hashes BCrypt. En `fragments/comunes.html` está marcado dónde
va el nombre del usuario y cómo esconder enlaces por rol.

Lo importante no es solo el login: es que **el manager de una tienda no vea los
datos de otra**. Ese filtrado es lo que hace sólida la gestión de roles.

### Integrante 4 — el caso transaccional
Crea `VentaService` en `service/` con un método `@Transactional`:

```java
@Transactional
public Venta registrarVenta(Venta venta, List<VentaDetalle> detalles) {
    // 1. validar que el vendedor pertenece a esa tienda
    // 2. por cada detalle:
    //      stockRepo.findParaActualizar(idProducto, idTienda)  <-- con bloqueo
    //      si cantidad < solicitado -> lanzar excepcion (rollback)
    //      descontar y guardar
    // 3. calcular total, guardar cabecera + detalle
    // 4. si algo falla, la anotacion revierte todo
}
```

Usa **siempre** `stockRepo.findParaActualizar(...)`, nunca el `findBy` normal.
Ese lleva `PESSIMISTIC_WRITE` y es lo que evita la sobreventa.

Para el video: dos navegadores vendiendo el mismo último producto a la vez. Uno
debe fallar limpio, sin dejar el stock en negativo.

### Integrante 5 — mantenimientos y consultas
Tres CRUD (productos, empleados y tiendas, proveedores) y dos consultas.
Los repositorios ya tienen todo lo que necesitas:

| Necesitas | Método listo |
|---|---|
| Mantenimiento de productos | `ProductoRepository` completo |
| Mantenimiento de empleados | `UsuarioRepository.findByTiendaIdTienda(...)` |
| Mantenimiento de proveedores | `ProveedorRepository` completo |
| Consulta de ventas | `VentaRepository.buscarPorFechaYTienda(...)` |
| Consulta de stock bajo | `StockRepository.findStockBajo(...)` |

Copia `demo/productos.html` para el listado y agrégale el formulario. Para las
bajas usa el campo `estado`, no borres filas: hay claves foráneas que lo impiden.

### Integrante 6 — reportes y panel
Descomenta las dependencias de JasperReports en el `pom.xml`.

| Necesitas | Método listo |
|---|---|
| Reporte de rentabilidad | `VentaDetalleRepository.rentabilidadPorProducto()` |
| Panel: ingresos por mes | `VentaRepository.totalPorMes(...)` |
| Panel: egresos por mes | `CompraRepository.totalPorMes(...)` + `GastoRepository.totalPorMes(...)` |
| Panel: gastos por categoría | `GastoRepository.totalPorCategoria(...)` |
| Panel: ventas por tienda | `VentaRepository.totalPorTienda()` |

Devuelven `List<Object[]>`; cada fila trae los valores en el orden que indica el
comentario del método.

**Dependes del integrante 4**: sin ventas registradas no hay datos que graficar.
Mientras tanto, diseña los `.jrxml` en Jaspersoft Studio con datos a mano.

## 6. Reglas del repositorio

- **Nadie trabaja sobre `main`.** Cada uno en su rama: `feature/mantenimientos`,
  `feature/seguridad`, etc.
- **Commits con tu propio usuario.** La plantilla exige evidencia de
  participación por integrante y el historial de GitHub es lo más difícil de
  improvisar a última hora.
- **No cambies `spring.jpa.hibernate.ddl-auto` a `update`.** Está en `validate`
  a propósito: si una entidad no coincide con su tabla, la app falla al arrancar
  con un mensaje claro en vez de deformar la base de todos en silencio.
- Si agregas una columna, va **primero al script SQL** y después a la entidad.

## 7. Notas técnicas

**Sobre el material del curso.** Los PDFs del Tema 1 usan `javax.persistence`
con Java 11 y Hibernate 5 suelto. Este proyecto usa Spring Boot 3, que va con
Java 17 y `jakarta.persistence`. Las anotaciones son exactamente las mismas
(`@Entity`, `@Table`, `@Id`, `@ManyToOne`) — solo cambia el import. Si copian
código de las diapositivas, cambien `javax` por `jakarta`.

**Sobre la base de datos.** Está en MySQL porque es lo que usa el material del
curso. Si prefieren SQL Server (ya está instalado en una de nuestras máquinas),
el cambio son dos líneas: el driver en el `pom.xml` y la URL en
`application.properties`. Pero decídanlo ahora, no a mitad del proyecto.
