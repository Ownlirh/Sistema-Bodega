USE bodegadb;

CREATE TABLE IF NOT EXISTS tb_clientes (
    id_cliente INT NOT NULL AUTO_INCREMENT,
    nombres VARCHAR(255) NULL,
    apellidos VARCHAR(255) NULL,
    dni VARCHAR(255) NULL,
    telefono VARCHAR(255) NULL,
    direccion VARCHAR(255) NULL,
    estado BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id_cliente)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
