-- ============================================================================
-- Sistema de ventas para bodegas
-- Base de datos MySQL 8
-- Ejecutar completo: crea la BD, las tablas y los datos de prueba.
-- ============================================================================

DROP DATABASE IF EXISTS bodegadb;
CREATE DATABASE bodegadb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE bodegadb;

-- ============================================================================
-- SEGURIDAD Y ORGANIZACION
-- ============================================================================

CREATE TABLE tb_roles (
    id_rol      INT AUTO_INCREMENT PRIMARY KEY,
    nombre      VARCHAR(30) NOT NULL UNIQUE
);

CREATE TABLE tb_tiendas (
    id_tienda   INT AUTO_INCREMENT PRIMARY KEY,
    nombre      VARCHAR(60) NOT NULL,
    direccion   VARCHAR(120),
    distrito    VARCHAR(60),
    telefono    VARCHAR(15),
    estado      TINYINT(1) NOT NULL DEFAULT 1
);

-- Los empleados del sistema. El rol define que puede hacer,
-- la tienda define que datos ve. El administrador lleva id_tienda NULL
-- porque ve todas las tiendas.
CREATE TABLE tb_usuarios (
    id_usuario  INT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(45) NOT NULL UNIQUE,
    password    VARCHAR(100) NOT NULL,
    nombres     VARCHAR(60) NOT NULL,
    apellidos   VARCHAR(60) NOT NULL,
    email       VARCHAR(80),
    dni         VARCHAR(8),
    id_rol      INT NOT NULL,
    id_tienda   INT NULL,
    estado      TINYINT(1) NOT NULL DEFAULT 1,
    CONSTRAINT fk_usuario_rol    FOREIGN KEY (id_rol)    REFERENCES tb_roles(id_rol),
    CONSTRAINT fk_usuario_tienda FOREIGN KEY (id_tienda) REFERENCES tb_tiendas(id_tienda)
);

-- ============================================================================
-- CATALOGO DE PRODUCTOS
-- ============================================================================

CREATE TABLE tb_categorias (
    id_categoria INT AUTO_INCREMENT PRIMARY KEY,
    nombre       VARCHAR(45) NOT NULL,
    estado       TINYINT(1) NOT NULL DEFAULT 1
);

CREATE TABLE tb_proveedores (
    id_proveedor INT AUTO_INCREMENT PRIMARY KEY,
    razon_social VARCHAR(80) NOT NULL,
    ruc          VARCHAR(11),
    telefono     VARCHAR(15),
    email        VARCHAR(80),
    estado       TINYINT(1) NOT NULL DEFAULT 1
);

-- precio_compra y precio_venta viven aqui porque de su diferencia
-- sale el margen que alimenta el panel de tendencias.
CREATE TABLE tb_productos (
    id_producto   INT AUTO_INCREMENT PRIMARY KEY,
    codigo        VARCHAR(20) NOT NULL UNIQUE,
    descripcion   VARCHAR(80) NOT NULL,
    unidad_medida VARCHAR(15) NOT NULL DEFAULT 'UNIDAD',
    precio_compra DECIMAL(10,2) NOT NULL,
    precio_venta  DECIMAL(10,2) NOT NULL,
    stock_minimo  INT NOT NULL DEFAULT 5,
    id_categoria  INT NOT NULL,
    id_proveedor  INT NULL,
    estado        TINYINT(1) NOT NULL DEFAULT 1,
    CONSTRAINT fk_producto_categoria FOREIGN KEY (id_categoria) REFERENCES tb_categorias(id_categoria),
    CONSTRAINT fk_producto_proveedor FOREIGN KEY (id_proveedor) REFERENCES tb_proveedores(id_proveedor)
);

-- El stock es POR TIENDA, no global: cada bodega tiene su propio inventario.
-- La columna version implementa el bloqueo optimista de JPA y es lo que evita
-- que dos cajas vendan la misma ultima unidad al mismo tiempo. No borrarla.
CREATE TABLE tb_stock (
    id_stock    INT AUTO_INCREMENT PRIMARY KEY,
    id_producto INT NOT NULL,
    id_tienda   INT NOT NULL,
    cantidad    INT NOT NULL DEFAULT 0,
    version     BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_stock_producto FOREIGN KEY (id_producto) REFERENCES tb_productos(id_producto),
    CONSTRAINT fk_stock_tienda   FOREIGN KEY (id_tienda)   REFERENCES tb_tiendas(id_tienda),
    CONSTRAINT uq_stock_producto_tienda UNIQUE (id_producto, id_tienda)
);

-- ============================================================================
-- INGRESOS: VENTAS
-- ============================================================================

CREATE TABLE tb_ventas (
    id_venta   INT AUTO_INCREMENT PRIMARY KEY,
    fecha      DATETIME NOT NULL,
    id_tienda  INT NOT NULL,
    id_usuario INT NOT NULL,
    total      DECIMAL(10,2) NOT NULL DEFAULT 0,
    estado     TINYINT(1) NOT NULL DEFAULT 1,
    CONSTRAINT fk_venta_tienda  FOREIGN KEY (id_tienda)  REFERENCES tb_tiendas(id_tienda),
    CONSTRAINT fk_venta_usuario FOREIGN KEY (id_usuario) REFERENCES tb_usuarios(id_usuario)
);

-- precio_unitario se guarda copiado del producto al momento de vender.
-- Si manana sube el precio, la venta de ayer no debe cambiar.
CREATE TABLE tb_venta_detalle (
    id_detalle      INT AUTO_INCREMENT PRIMARY KEY,
    id_venta        INT NOT NULL,
    id_producto     INT NOT NULL,
    cantidad        INT NOT NULL,
    precio_unitario DECIMAL(10,2) NOT NULL,
    subtotal        DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_detventa_venta    FOREIGN KEY (id_venta)    REFERENCES tb_ventas(id_venta),
    CONSTRAINT fk_detventa_producto FOREIGN KEY (id_producto) REFERENCES tb_productos(id_producto)
);

-- ============================================================================
-- EGRESOS: COMPRAS A PROVEEDORES
-- ============================================================================

CREATE TABLE tb_compras (
    id_compra    INT AUTO_INCREMENT PRIMARY KEY,
    fecha        DATETIME NOT NULL,
    id_tienda    INT NOT NULL,
    id_proveedor INT NOT NULL,
    id_usuario   INT NOT NULL,
    total        DECIMAL(10,2) NOT NULL DEFAULT 0,
    estado       TINYINT(1) NOT NULL DEFAULT 1,
    CONSTRAINT fk_compra_tienda    FOREIGN KEY (id_tienda)    REFERENCES tb_tiendas(id_tienda),
    CONSTRAINT fk_compra_proveedor FOREIGN KEY (id_proveedor) REFERENCES tb_proveedores(id_proveedor),
    CONSTRAINT fk_compra_usuario   FOREIGN KEY (id_usuario)   REFERENCES tb_usuarios(id_usuario)
);

CREATE TABLE tb_compra_detalle (
    id_detalle      INT AUTO_INCREMENT PRIMARY KEY,
    id_compra       INT NOT NULL,
    id_producto     INT NOT NULL,
    cantidad        INT NOT NULL,
    precio_unitario DECIMAL(10,2) NOT NULL,
    subtotal        DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_detcompra_compra   FOREIGN KEY (id_compra)   REFERENCES tb_compras(id_compra),
    CONSTRAINT fk_detcompra_producto FOREIGN KEY (id_producto) REFERENCES tb_productos(id_producto)
);

-- ============================================================================
-- EGRESOS: GASTOS OPERATIVOS
-- Tabla simple a proposito. No es un modulo contable: solo fecha,
-- categoria y monto, que es lo que necesita el panel de tendencias.
-- ============================================================================

CREATE TABLE tb_categorias_gasto (
    id_categoria_gasto INT AUTO_INCREMENT PRIMARY KEY,
    nombre             VARCHAR(45) NOT NULL
);

CREATE TABLE tb_gastos (
    id_gasto           INT AUTO_INCREMENT PRIMARY KEY,
    fecha              DATE NOT NULL,
    id_tienda          INT NOT NULL,
    id_categoria_gasto INT NOT NULL,
    descripcion        VARCHAR(120),
    monto              DECIMAL(10,2) NOT NULL,
    id_usuario         INT NOT NULL,
    CONSTRAINT fk_gasto_tienda    FOREIGN KEY (id_tienda)          REFERENCES tb_tiendas(id_tienda),
    CONSTRAINT fk_gasto_categoria FOREIGN KEY (id_categoria_gasto) REFERENCES tb_categorias_gasto(id_categoria_gasto),
    CONSTRAINT fk_gasto_usuario   FOREIGN KEY (id_usuario)         REFERENCES tb_usuarios(id_usuario)
);

-- ============================================================================
-- DATOS DE PRUEBA
-- ============================================================================

INSERT INTO tb_roles (nombre) VALUES
    ('ADMINISTRADOR'), ('MANAGER'), ('VENDEDOR');

INSERT INTO tb_tiendas (nombre, direccion, distrito, telefono) VALUES
    ('Bodega Central',  'Av. Balta 450',        'Chiclayo',   '074521100'),
    ('Bodega Norte',    'Calle Los Cedros 120', 'La Victoria','074521200'),
    ('Bodega La Union', 'Av. Grau 880',         'Jose L. Ortiz','074521300');

-- IMPORTANTE: estas claves estan en texto plano solo para poder probar.
-- El integrante 3 debe reemplazarlas por hashes BCrypt al montar el login.
INSERT INTO tb_usuarios (username, password, nombres, apellidos, email, dni, id_rol, id_tienda) VALUES
    ('admin',    '123456', 'Ana',   'Torres',  'admin@bodega.pe',    '40011122', 1, NULL),
    ('manager1', '123456', 'Luis',  'Ramirez', 'manager1@bodega.pe', '40011123', 2, 1),
    ('manager2', '123456', 'Rosa',  'Diaz',    'manager2@bodega.pe', '40011124', 2, 2),
    ('manager3', '123456', 'Elsa',  'Vargas',  'manager3@bodega.pe', '40011128', 2, 3),
    ('vendedor1','123456', 'Jorge', 'Castro',  'vend1@bodega.pe',    '40011125', 3, 1),
    ('vendedor2','123456', 'Maria', 'Flores',  'vend2@bodega.pe',    '40011126', 3, 1),
    ('vendedor3','123456', 'Pedro', 'Quispe',  'vend3@bodega.pe',    '40011127', 3, 2),
    ('vendedor4','123456', 'Carla', 'Mendoza', 'vend4@bodega.pe',    '40011129', 3, 3);

INSERT INTO tb_categorias (nombre) VALUES
    ('Abarrotes'), ('Bebidas'), ('Lacteos'), ('Limpieza'), ('Snacks'), ('Golosinas');

INSERT INTO tb_proveedores (razon_social, ruc, telefono, email) VALUES
    ('Distribuidora Norte SAC', '20123456781', '074998877', 'ventas@dnorte.pe'),
    ('Alicorp Distribucion',    '20123456782', '014445566', 'pedidos@alicorpd.pe'),
    ('Gloria Regional EIRL',    '20123456783', '074556677', 'contacto@gloriareg.pe');

-- El margen de cada producto es precio_venta - precio_compra.
-- Ojo con 'Arroz Costeno 5kg': se vende casi al costo a proposito,
-- para que el reporte de rentabilidad tenga un caso interesante que mostrar.
INSERT INTO tb_productos (codigo, descripcion, unidad_medida, precio_compra, precio_venta, stock_minimo, id_categoria, id_proveedor) VALUES
    ('P001','Arroz Costeno 5kg',      'BOLSA',  22.50, 23.90, 10, 1, 2),
    ('P002','Aceite Primor 1L',       'UNIDAD',  8.20, 10.50, 10, 1, 2),
    ('P003','Azucar Rubia 1kg',       'BOLSA',   3.60,  4.80, 15, 1, 1),
    ('P004','Fideos Don Vittorio',    'UNIDAD',  2.10,  3.20, 20, 1, 2),
    ('P005','Atun Florida lata',      'UNIDAD',  4.30,  6.00, 15, 1, 1),
    ('P006','Inca Kola 1.5L',         'UNIDAD',  5.10,  7.50, 20, 2, 1),
    ('P007','Coca Cola 1.5L',         'UNIDAD',  5.30,  7.50, 20, 2, 1),
    ('P008','Agua San Luis 625ml',    'UNIDAD',  1.00,  1.80, 30, 2, 1),
    ('P009','Cerveza Pilsen 650ml',   'UNIDAD',  5.60,  8.50, 24, 2, 1),
    ('P010','Leche Gloria 400g',      'UNIDAD',  3.40,  4.70, 20, 3, 3),
    ('P011','Yogurt Gloria 1L',       'UNIDAD',  6.20,  8.90, 10, 3, 3),
    ('P012','Queso fresco 250g',      'UNIDAD',  7.50, 11.00,  8, 3, 3),
    ('P013','Detergente Bolivar 1kg', 'BOLSA',   9.80, 13.50, 10, 4, 2),
    ('P014','Lejia Clorox 1L',        'UNIDAD',  3.90,  5.90, 12, 4, 2),
    ('P015','Papel higienico x4',     'PAQUETE', 5.40,  8.00, 15, 4, 1),
    ('P016','Papas Lays 145g',        'UNIDAD',  4.20,  6.50, 15, 5, 1),
    ('P017','Galleta Soda Field',     'PAQUETE', 1.30,  2.00, 25, 5, 2),
    ('P018','Chocolate Sublime',      'UNIDAD',  1.50,  2.50, 30, 6, 2),
    ('P019','Caramelos Halls',        'UNIDAD',  1.20,  2.00, 30, 6, 1),
    ('P020','Chizitos',               'UNIDAD',  0.80,  1.50, 40, 5, 1);

-- Stock inicial: todos los productos en las tres tiendas.
INSERT INTO tb_stock (id_producto, id_tienda, cantidad)
SELECT p.id_producto, t.id_tienda, FLOOR(20 + RAND() * 60)
FROM tb_productos p CROSS JOIN tb_tiendas t;

-- Un producto deliberadamente bajo de stock, para probar la alerta de minimo.
UPDATE tb_stock SET cantidad = 2 WHERE id_producto = 1 AND id_tienda = 1;

INSERT INTO tb_categorias_gasto (nombre) VALUES
    ('Alquiler'), ('Servicios'), ('Sueldos'), ('Transporte'), ('Mantenimiento'), ('Otros');

INSERT INTO tb_gastos (fecha, id_tienda, id_categoria_gasto, descripcion, monto, id_usuario) VALUES
    ('2026-07-05', 1, 1, 'Alquiler del local julio',  1200.00, 2),
    ('2026-07-08', 1, 2, 'Luz y agua julio',           340.50, 2),
    ('2026-07-30', 1, 3, 'Sueldos julio',             2600.00, 2),
    ('2026-08-05', 1, 1, 'Alquiler del local agosto', 1200.00, 2),
    ('2026-08-09', 1, 2, 'Luz y agua agosto',          368.20, 2),
    ('2026-08-30', 1, 3, 'Sueldos agosto',            2600.00, 2),
    ('2026-07-05', 2, 1, 'Alquiler del local julio',   950.00, 3),
    ('2026-07-08', 2, 2, 'Luz y agua julio',           275.00, 3),
    ('2026-08-05', 2, 1, 'Alquiler del local agosto',  950.00, 3),
    ('2026-08-12', 2, 4, 'Flete de mercaderia',        180.00, 3),
    ('2026-09-05', 1, 1, 'Alquiler del local setiembre',1200.00, 2),
    ('2026-09-08', 1, 2, 'Luz y agua setiembre',         352.40, 2),
    ('2026-09-05', 2, 1, 'Alquiler del local setiembre', 950.00, 3),
    ('2026-07-05', 3, 1, 'Alquiler del local julio',     880.00, 4),
    ('2026-08-05', 3, 1, 'Alquiler del local agosto',    880.00, 4),
    ('2026-09-05', 3, 1, 'Alquiler del local setiembre', 880.00, 4),
    ('2026-07-30', 3, 3, 'Sueldos julio',               1900.00, 4),
    ('2026-08-30', 3, 3, 'Sueldos agosto',              1900.00, 4),
    ('2026-08-18', 3, 5, 'Reparacion de refrigerador',   420.00, 4);

-- ============================================================================
-- COMPROBACION
-- ============================================================================
SELECT 'roles' AS tabla, COUNT(*) AS filas FROM tb_roles
UNION ALL SELECT 'tiendas',    COUNT(*) FROM tb_tiendas
UNION ALL SELECT 'usuarios',   COUNT(*) FROM tb_usuarios
UNION ALL SELECT 'categorias', COUNT(*) FROM tb_categorias
UNION ALL SELECT 'proveedores',COUNT(*) FROM tb_proveedores
UNION ALL SELECT 'productos',  COUNT(*) FROM tb_productos
UNION ALL SELECT 'stock',      COUNT(*) FROM tb_stock
UNION ALL SELECT 'gastos',     COUNT(*) FROM tb_gastos;
