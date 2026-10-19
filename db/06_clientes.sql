USE bodegadb;

CREATE TABLE IF NOT EXISTS tb_clientes (
    id_cliente INT NOT NULL AUTO_INCREMENT,
    nombres VARCHAR(60) NOT NULL,
    apellidos VARCHAR(60) NOT NULL,
    dni VARCHAR(8) NULL,
    telefono VARCHAR(15) NULL,
    direccion VARCHAR(120) NULL,
    estado BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id_cliente)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
