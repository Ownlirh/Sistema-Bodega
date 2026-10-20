
USE bodegadb;

-- ==========================================================
-- MODULO: CLIENTES Y CREDITO - INTEGRANTE 2
-- Registra fiados y sus pagos sin modificar tb_ventas.
-- ==========================================================

CREATE TABLE IF NOT EXISTS tb_fiados (
    id_fiado INT NOT NULL AUTO_INCREMENT,
    id_cliente INT NOT NULL,
    descripcion VARCHAR(200) NOT NULL,
    fecha DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    monto_inicial DECIMAL(10,2) NOT NULL,
    saldo_pendiente DECIMAL(10,2) NOT NULL,
    estado VARCHAR(15) NOT NULL DEFAULT 'PENDIENTE',

    PRIMARY KEY (id_fiado),

    INDEX idx_fiados_cliente (id_cliente),
    INDEX idx_fiados_estado (estado),

    CONSTRAINT fk_fiados_cliente
        FOREIGN KEY (id_cliente)
        REFERENCES tb_clientes(id_cliente)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,

    CONSTRAINT chk_fiados_monto
        CHECK (monto_inicial > 0),

    CONSTRAINT chk_fiados_saldo
        CHECK (
            saldo_pendiente >= 0
            AND saldo_pendiente <= monto_inicial
        )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


CREATE TABLE IF NOT EXISTS tb_pagos_fiado (
    id_pago INT NOT NULL AUTO_INCREMENT,
    id_fiado INT NOT NULL,
    fecha DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    monto DECIMAL(10,2) NOT NULL,
    observacion VARCHAR(200),

    PRIMARY KEY (id_pago),

    INDEX idx_pagos_fiado (id_fiado),

    CONSTRAINT fk_pagos_fiado
        FOREIGN KEY (id_fiado)
        REFERENCES tb_fiados(id_fiado)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,

    CONSTRAINT chk_pagos_monto
        CHECK (monto > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
