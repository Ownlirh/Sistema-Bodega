-- Crea solo las tablas faltantes; conserva todas las tablas y sus datos.
USE bodegadb;
CREATE TABLE IF NOT EXISTS tb_kardex (
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

CREATE TABLE IF NOT EXISTS tb_transferencias (
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

CREATE TABLE IF NOT EXISTS tb_ajustes (
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
