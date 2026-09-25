-- ============================================================================
-- MODULO INVENTARIO Y LOGISTICA (integrante 7)
--
-- Se ejecuta DESPUES de 01_script_bd.sql (y de 02_datos_demo.sql si se usa).
-- Solo AGREGA tablas nuevas: no modifica ni borra las tablas del equipo.
--
-- Tablas:
--   tb_kardex          historial de todos los movimientos de stock
--   tb_transferencias  envio de un producto de una tienda a otra
--   tb_ajustes         correccion del stock despues de un conteo fisico
-- ============================================================================

USE bodegadb;

DROP TABLE IF EXISTS tb_kardex;
DROP TABLE IF EXISTS tb_transferencias;
DROP TABLE IF EXISTS tb_ajustes;

-- ----------------------------------------------------------------------------
-- KARDEX: una fila por cada vez que cambia el stock de un producto en una tienda.
--   tipo          : VENTA, COMPRA, DEVOLUCION, TRANSFERENCIA_SALIDA,
--                   TRANSFERENCIA_ENTRADA o AJUSTE
--   cantidad      : positiva si entra mercaderia, negativa si sale
--   stock_antes   : stock que habia antes del movimiento
--   stock_despues : stock que quedo (stock_antes + cantidad)
--   referencia    : texto corto, por ejemplo 'Transferencia #3'
-- ----------------------------------------------------------------------------
CREATE TABLE tb_kardex (
    id_kardex     INT AUTO_INCREMENT PRIMARY KEY,
    fecha         DATETIME NOT NULL,
    id_producto   INT NOT NULL,
    id_tienda     INT NOT NULL,
    tipo          VARCHAR(25) NOT NULL,
    cantidad      INT NOT NULL,
    stock_antes   INT NOT NULL,
    stock_despues INT NOT NULL,
    referencia    VARCHAR(100),
    id_usuario    INT NOT NULL,
    CONSTRAINT fk_kardex_producto FOREIGN KEY (id_producto) REFERENCES tb_productos(id_producto),
    CONSTRAINT fk_kardex_tienda   FOREIGN KEY (id_tienda)   REFERENCES tb_tiendas(id_tienda),
    CONSTRAINT fk_kardex_usuario  FOREIGN KEY (id_usuario)  REFERENCES tb_usuarios(id_usuario)
);

-- ----------------------------------------------------------------------------
-- TRANSFERENCIAS: un producto pasa de la tienda origen a la tienda destino.
-- Version simple: una transferencia = un producto.
-- Al registrarla se generan 2 filas en tb_kardex (salida y entrada).
-- ----------------------------------------------------------------------------
CREATE TABLE tb_transferencias (
    id_transferencia  INT AUTO_INCREMENT PRIMARY KEY,
    fecha             DATETIME NOT NULL,
    id_producto       INT NOT NULL,
    id_tienda_origen  INT NOT NULL,
    id_tienda_destino INT NOT NULL,
    cantidad          INT NOT NULL,
    observacion       VARCHAR(150),
    id_usuario        INT NOT NULL,
    CONSTRAINT fk_transf_producto FOREIGN KEY (id_producto)       REFERENCES tb_productos(id_producto),
    CONSTRAINT fk_transf_origen   FOREIGN KEY (id_tienda_origen)  REFERENCES tb_tiendas(id_tienda),
    CONSTRAINT fk_transf_destino  FOREIGN KEY (id_tienda_destino) REFERENCES tb_tiendas(id_tienda),
    CONSTRAINT fk_transf_usuario  FOREIGN KEY (id_usuario)        REFERENCES tb_usuarios(id_usuario)
);

-- ----------------------------------------------------------------------------
-- AJUSTES POR CONTEO FISICO: el empleado cuenta lo que hay en el estante.
--   stock_sistema : lo que decia tb_stock
--   stock_fisico  : lo que se conto
--   diferencia    : stock_fisico - stock_sistema (puede ser negativa)
-- Al registrarlo, tb_stock queda igual a stock_fisico y se genera 1 fila
-- en tb_kardex con tipo AJUSTE.
-- ----------------------------------------------------------------------------
CREATE TABLE tb_ajustes (
    id_ajuste     INT AUTO_INCREMENT PRIMARY KEY,
    fecha         DATETIME NOT NULL,
    id_producto   INT NOT NULL,
    id_tienda     INT NOT NULL,
    stock_sistema INT NOT NULL,
    stock_fisico  INT NOT NULL,
    diferencia    INT NOT NULL,
    motivo        VARCHAR(150) NOT NULL,
    id_usuario    INT NOT NULL,
    CONSTRAINT fk_ajuste_producto FOREIGN KEY (id_producto) REFERENCES tb_productos(id_producto),
    CONSTRAINT fk_ajuste_tienda   FOREIGN KEY (id_tienda)   REFERENCES tb_tiendas(id_tienda),
    CONSTRAINT fk_ajuste_usuario  FOREIGN KEY (id_usuario)  REFERENCES tb_usuarios(id_usuario)
);

-- Verificacion rapida
SHOW TABLES LIKE 'tb_kardex';
SHOW TABLES LIKE 'tb_transferencias';
SHOW TABLES LIKE 'tb_ajustes';
