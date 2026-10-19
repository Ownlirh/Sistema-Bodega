-- ============================================================================
-- MIGRACIÓN 06: Seguridad, bitácora y caja diaria
-- Ejecutar UNA VEZ sobre la base bodegadb ya creada con 01_script_bd.sql.
-- No borra ni reinicia la base de datos.
-- ============================================================================
USE bodegadb;

CREATE TABLE IF NOT EXISTS tb_auditoria (
    id_auditoria BIGINT AUTO_INCREMENT PRIMARY KEY,
    fecha DATETIME NOT NULL,
    id_usuario INT NULL,
    id_tienda INT NULL,
    username VARCHAR(45) NOT NULL,
    accion VARCHAR(80) NOT NULL,
    detalle VARCHAR(500) NULL,
    direccion_ip VARCHAR(45) NULL,
    resultado VARCHAR(15) NOT NULL,
    INDEX idx_auditoria_fecha (fecha),
    INDEX idx_auditoria_tienda_fecha (id_tienda, fecha),
    INDEX idx_auditoria_usuario_fecha (id_usuario, fecha),
    CONSTRAINT fk_auditoria_usuario FOREIGN KEY (id_usuario) REFERENCES tb_usuarios(id_usuario) ON DELETE SET NULL,
    CONSTRAINT fk_auditoria_tienda FOREIGN KEY (id_tienda) REFERENCES tb_tiendas(id_tienda) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS tb_cajas (
    id_caja BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_tienda INT NOT NULL,
    id_usuario_apertura INT NOT NULL,
    fecha_apertura DATETIME NOT NULL,
    monto_inicial DECIMAL(12,2) NOT NULL DEFAULT 0,
    estado VARCHAR(15) NOT NULL DEFAULT 'ABIERTA',
    id_usuario_cierre INT NULL,
    fecha_cierre DATETIME NULL,
    total_ventas DECIMAL(12,2) NULL,
    total_ingresos DECIMAL(12,2) NULL,
    total_egresos DECIMAL(12,2) NULL,
    efectivo_esperado DECIMAL(12,2) NULL,
    efectivo_contado DECIMAL(12,2) NULL,
    diferencia DECIMAL(12,2) NULL,
    observacion_cierre VARCHAR(500) NULL,
    INDEX idx_caja_tienda_estado (id_tienda, estado),
    INDEX idx_caja_apertura (fecha_apertura),
    CONSTRAINT fk_caja_tienda FOREIGN KEY (id_tienda) REFERENCES tb_tiendas(id_tienda),
    CONSTRAINT fk_caja_usuario_apertura FOREIGN KEY (id_usuario_apertura) REFERENCES tb_usuarios(id_usuario),
    CONSTRAINT fk_caja_usuario_cierre FOREIGN KEY (id_usuario_cierre) REFERENCES tb_usuarios(id_usuario)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS tb_caja_movimientos (
    id_movimiento BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_caja BIGINT NOT NULL,
    id_usuario INT NOT NULL,
    fecha DATETIME NOT NULL,
    tipo VARCHAR(10) NOT NULL,
    concepto VARCHAR(120) NOT NULL,
    monto DECIMAL(12,2) NOT NULL,
    INDEX idx_movimiento_caja_fecha (id_caja, fecha),
    CONSTRAINT chk_movimiento_tipo CHECK (tipo IN ('ENTRADA', 'SALIDA')),
    CONSTRAINT chk_movimiento_monto CHECK (monto > 0),
    CONSTRAINT fk_movimiento_caja FOREIGN KEY (id_caja) REFERENCES tb_cajas(id_caja),
    CONSTRAINT fk_movimiento_usuario FOREIGN KEY (id_usuario) REFERENCES tb_usuarios(id_usuario)
) ENGINE=InnoDB;

-- NOTA: los usuarios demo actuales pueden tener contraseñas en texto plano.
-- El primer inicio de sesión válido las convierte automáticamente a BCrypt.
-- Para un entorno real, deshabilitar o reemplazar usuarios demo antes de publicar.
