-- Actualización aditiva: no borra tablas ni transforma el stock existente.
USE bodegadb;
CREATE TABLE IF NOT EXISTS tb_lotes (
    id_lote INT AUTO_INCREMENT PRIMARY KEY,
    id_producto INT NOT NULL,
    id_tienda INT NOT NULL,
    codigo VARCHAR(60) NOT NULL,
    vencimiento DATE NOT NULL,
    disponible INT NOT NULL,
    cantidad_inicial INT NOT NULL,
    fecha_registro DATETIME NOT NULL,
    id_usuario INT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_lote_producto FOREIGN KEY (id_producto) REFERENCES tb_productos(id_producto),
    CONSTRAINT fk_lote_tienda FOREIGN KEY (id_tienda) REFERENCES tb_tiendas(id_tienda),
    CONSTRAINT fk_lote_usuario FOREIGN KEY (id_usuario) REFERENCES tb_usuarios(id_usuario),
    CONSTRAINT ck_lote_saldo CHECK (disponible >= 0 AND disponible <= cantidad_inicial),
    INDEX idx_lote_producto_tienda (id_producto, id_tienda),
    INDEX idx_lote_vencimiento (vencimiento)
);
CREATE TABLE IF NOT EXISTS tb_mermas (
    id_merma INT AUTO_INCREMENT PRIMARY KEY,
    id_lote INT NOT NULL,
    fecha DATETIME NOT NULL,
    cantidad INT NOT NULL,
    motivo VARCHAR(20) NOT NULL,
    descripcion VARCHAR(150) NOT NULL,
    id_usuario INT NOT NULL,
    CONSTRAINT fk_merma_lote FOREIGN KEY (id_lote) REFERENCES tb_lotes(id_lote),
    CONSTRAINT fk_merma_usuario FOREIGN KEY (id_usuario) REFERENCES tb_usuarios(id_usuario),
    CONSTRAINT ck_merma_cantidad CHECK (cantidad > 0)
);
